import cv2
import imageio
import json
import boto3
import os
import time
import logging
import torch
from ultralytics import YOLO
from pathlib import Path
from urllib.parse import urlparse
from botocore.exceptions import WaiterError
import backoff 
from botocore.exceptions import ConnectionError, ClientError

MAX_RETRIES = 3
BASE_DELAY = 1
MAX_DELAY = 30

# get SQS queue uri from parameter store
ssm = boto3.client('ssm', region_name='us-east-1')
sqs_queue_url = ssm.get_parameter(Name='/video-analytics/newvideo-sqs')['Parameter']['Value']

@backoff.on_exception(
    backoff.expo,
    (ConnectionError, ClientError),
    max_tries=MAX_RETRIES,
    max_time=300,  # Maximum total time to try in seconds
    base=2,  # Base factor for exponential backoff
)

def receive_sqs_message(queue_url):
    """Receive message from SQS with exponential backoff retry."""
    return sqs.receive_message(
        QueueUrl=queue_url,
        MaxNumberOfMessages=1,
        WaitTimeSeconds=20,  # Reduced from default to avoid timeouts
        VisibilityTimeout=900  # 15 minutes
    )

@backoff.on_exception(
    backoff.expo,
    (ConnectionError, ClientError),
    max_tries=MAX_RETRIES,
    max_time=300,
    base=2,
)
def delete_sqs_message(queue_url, receipt_handle):
    """Delete message from SQS with exponential backoff retry."""
    return sqs.delete_message(
        QueueUrl=queue_url,
        ReceiptHandle=receipt_handle
    )

@backoff.on_exception(
    backoff.expo,
    (ConnectionError, ClientError),
    max_tries=MAX_RETRIES,
    max_time=300,
    base=2,
)

def extend_message_visibility(queue_url, receipt_handle, visibility_timeout):
    """Extend message visibility timeout with exponential backoff retry."""
    return sqs.change_message_visibility(
        QueueUrl=queue_url,
        ReceiptHandle=receipt_handle,
        VisibilityTimeout=visibility_timeout
    )

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# Initialize AWS clients
sqs = boto3.client('sqs', region_name='us-east-1')
s3 = boto3.client('s3', region_name='us-east-1')

def check_cuda_availability():
    """Check if CUDA is available and return device configuration."""
    if torch.cuda.is_available():
        device = "cuda"
        logger.info(f"CUDA is available. Using GPU: {torch.cuda.get_device_name(0)}")
    else:
        device = "cpu"
        logger.info("CUDA is not available. Using CPU")
    return device

def parse_s3_url(s3_url):
    """Parse S3 URL into bucket and key."""
    parsed = urlparse(s3_url)
    bucket = parsed.netloc
    key = parsed.path.lstrip('/')
    return bucket, key

def download_from_s3(bucket, key, local_path):
    """Download file from S3."""
    try:
        s3.download_file(bucket, key, local_path)
        logger.info(f"Successfully downloaded file from s3://{bucket}/{key} to {local_path}")
    except Exception as e:
        logger.error(f"Error downloading file from S3: {str(e)}", exc_info=True)
        raise

def upload_to_s3(bucket, key, local_path):
    """Upload file to S3 and wait for it to be available."""
    try:
        # Upload the file
        s3.upload_file(local_path, bucket, key)
        
        # Create a waiter
        waiter = s3.get_waiter('object_exists')
        
        try:
            # Wait for the object to be available
            waiter.wait(Bucket=bucket, Key=key)
            logger.info(f"File {key} successfully uploaded to {bucket} and is now available.")
        except WaiterError as e:
            logger.error(f"Error waiting for file to be available: {e}", exc_info=True)
            raise

        return True
    except Exception as e:
        logger.error(f"Error uploading file to S3: {str(e)}", exc_info=True)
        raise

def change_format(results, ts, person_only):
    try:
        object_json = []
        if not hasattr(results, 'boxes') or len(results.boxes) == 0:
            logger.warning("No boxes found in results")
            return object_json

        for i, obj in enumerate(results.boxes):
            try:
                if len(obj.xywhn) == 0:
                    continue
                x_center, y_center, width, height = obj.xywhn[0]
                # Calculate Left and Top from center
                left = x_center - (width / 2)
                top = y_center - (height / 2)
                
                if not hasattr(obj, 'cls'):
                    logger.warning(f"Object {i} has no 'cls' attribute")
                    continue
                
                # Get the class name
                class_id = int(obj.cls[0])
                obj_name = results.names[class_id]
                
                # Create dictionary for each object detected
                if (person_only and obj_name == "person") or not person_only:
                    obj_data = {
                        obj_name: {
                            "boundingBox": {
                                "height": float(height),
                                "left": float(left),
                                "top": float(top),
                                "width": float(width)
                            },
                            "index": int(obj.id) if hasattr(obj, 'id') and obj.id is not None else i  # Object index
                        },
                        "timestamp": ts  # timestamp of the detected object
                    }
                    object_json.append(obj_data)
            except (IndexError, AttributeError, ValueError, TypeError) as e:
                logger.error(f"Error processing object {i}: {str(e)}", exc_info=True)
                continue

        return object_json
    except Exception as e:
        logger.error(f"Error in change_format: {str(e)}", exc_info=True)
        raise



def person_tracking(video_path, model, message, person_only=True, save_video=True):
    last_visibility_extension = time.time()
    visibility_extension_interval = 300  # 5 minutes in seconds

    logger.info(f"Starting person tracking for video: {video_path}")
    try:
        reader = imageio.get_reader(video_path)
        frames = []
        i = 0
        all_object_data = []
        file_name = Path(video_path).stem

        # Initialize last visibility extension time
        last_visibility_extension = time.time()
        visibility_extension_interval = 300  # 5 minutes in seconds

        for frame in reader:
            current_time = time.time()
            if current_time - last_visibility_extension >= visibility_extension_interval:
                try:
                    extend_message_visibility(
                        sqs_queue_url,
                        message['ReceiptHandle'],
                        900  # 15 minutes
                    )
                    last_visibility_extension = current_time
                    logger.info("Extended message visibility timeout")
                except Exception as e:
                    logger.warning(f"Failed to extend message visibility: {str(e)}")

            frame_bgr = cv2.cvtColor(frame, cv2.COLOR_RGB2BGR)
            try:
                conf = 0.2
                iou = 0.5
                results = model.track(frame_bgr, persist=True, conf=conf, iou=iou, show=False, tracker="bytetrack.yaml")

                object_json = change_format(results[0], i, person_only)
                all_object_data.append(object_json)

                annotated_frame = results[0].plot()
                frames.append(annotated_frame)
                i += 1

            except Exception as e:
                logger.error(f"Error processing frame {i}: {str(e)}", exc_info=True)
                break

        # Save outputs
        output_json_path = f'./tmp/{file_name}_output.json'
        # Create /tmp directory if it doesn't exist
        os.makedirs(os.path.dirname(output_json_path), exist_ok=True)
        with open(output_json_path, 'w') as file:
            json.dump(all_object_data, file, indent=4)
        logger.info(f"JSON output saved to {output_json_path}")
       
        if save_video:
            fourcc = cv2.VideoWriter_fourcc(*'mp4v')
            output_path = f"./tmp/{file_name}_annotated.mp4"
            # Create ./tmp directory if it doesn't exist
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            fps = reader.get_meta_data()['fps']
            frame_size = reader.get_meta_data()['size']
            video_writer = cv2.VideoWriter(output_path, fourcc, fps, frame_size)

            for frame in frames:
                video_writer.write(frame)
            video_writer.release()
            logger.info(f"Annotated video saved to {output_path}")

        return all_object_data, output_json_path, output_path if save_video else None

    except Exception as e:
        logger.error(f"Error in person_tracking: {str(e)}", exc_info=True)
        raise

def process_message(message, model):
    """Process a single SQS message."""
    try:
        # Parse message body
        message_body = json.loads(message['Body'])
        
        # Log the message body for debugging
        logger.debug(f"Received message body: {json.dumps(message_body, indent=2)}")
        
        # Check if 'Records' key exists
        if 'Records' not in message_body:
            logger.warning("Message does not contain 'Records' key. Attempting to process as direct S3 event.")
            s3_records = [message_body]  # Treat the entire message as a single record
        else:
            s3_records = message_body['Records']
        
        for record in s3_records:
            # Check if this is an S3 event
            if 's3' not in record:
                logger.warning(f"Record does not contain S3 information: {json.dumps(record, indent=2)}")
                continue
            
            # Get bucket and key from the S3 event
            input_bucket = record['s3']['bucket']['name']
            input_key = record['s3']['object']['key']
            
            # Skip if this is not a video file
            if not input_key.lower().endswith(('.mp4', '.avi', '.mov')):
                logger.info(f"Skipping non-video file: {input_key}")
                continue

            logger.info(f"Processing video from bucket: {input_bucket}, key: {input_key}")
            
            # Set up local paths
            local_input_path = f'/tmp/input_video_{int(time.time())}_{input_key.split("/")[-1]}'
            if not os.path.exists('/tmp'):
                os.makedirs('/tmp')
            
            # Download video from S3
            logger.info(f"Downloading video from s3://{input_bucket}/{input_key}")
            download_from_s3(input_bucket, input_key, local_input_path)
            
            # Process video - passing the message as a parameter
            logger.info("Processing video...")
            all_object_data, json_path, video_path = person_tracking(
                local_input_path, model, message, person_only=True, save_video=True
            )
            
            # Upload results to S3 (using same bucket as input)
            logger.info("Uploading results to S3...")
            output_json_key = f"output/{Path(input_key).stem}_output.json"
            output_video_key = f"output/{Path(input_key).stem}_annotated.mp4"
            
            upload_to_s3(input_bucket, output_json_key, json_path)
            if video_path:
                upload_to_s3(input_bucket, output_video_key, video_path)
            
            # Clean up local files
            os.remove(local_input_path)
            os.remove(json_path)
            if video_path:
                os.remove(video_path)
                
            logger.info("Processing complete!")
        
        return True
        
    except Exception as e:
        logger.error(f"Error processing message: {str(e)}", exc_info=True)
        return False



def main():
    try:
        # Get SQS URL from environment variable
        queue_url = sqs_queue_url
        
        # Check CUDA availability and set device
        device = check_cuda_availability()
        
        logger.info("Initializing YOLOv9 model...")
        model = YOLO('yolov9e-seg.pt')
        model.to(device)
        
        logger.info(f"Model loaded successfully on {device}")
        logger.info(f"Starting to poll messages from {queue_url}")
        
        while True:
            try:
                # Receive message from SQS with retry mechanism
                response = receive_sqs_message(queue_url)
                
                messages = response.get('Messages', [])
                
                for message in messages:
                    receipt_handle = message['ReceiptHandle']
                    
                    try:
                        # Process the message
                        success = process_message(message, model)
                        
                        # If processing was successful, delete the message from the queue
                        if success:
                            delete_sqs_message(queue_url, receipt_handle)
                            logger.info("Message processed and deleted from queue")
                    
                    except Exception as e:
                        logger.error(f"Error processing individual message: {str(e)}", exc_info=True)
                        # Don't raise the exception here to continue processing other messages
                        continue
                
                if not messages:
                    logger.debug("No messages found, continuing to poll...")
                    time.sleep(1)  # Add a small delay to prevent aggressive polling
                    
            except Exception as e:
                logger.error(f"Error in message processing loop: {str(e)}", exc_info=True)
                time.sleep(5)  # Wait before retrying
    
    except Exception as e:
        logger.error(f"Critical error in main function: {str(e)}", exc_info=True)
        raise

if __name__ == "__main__":
    main()
