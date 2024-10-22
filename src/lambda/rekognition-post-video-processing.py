import json
import boto3
import os
import re
import logging
from botocore.exceptions import ClientError

# Set up logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Retrieve values from environment variables
S3_BUCKET_NAME = os.environ.get('S3_BUCKET_NAME')
S3_REGION = os.environ.get('S3_REGION')
SQS_QUEUE_URL = os.environ.get('SQS_QUEUE_URL')

if not S3_BUCKET_NAME or not S3_REGION or not SQS_QUEUE_URL:
    raise ValueError("One or more environment variables are missing: S3_BUCKET_NAME, S3_REGION, SQS_QUEUE_URL")

# Initialize AWS clients
s3 = boto3.client('s3', region_name=S3_REGION)
rekognition = boto3.client('rekognition')
sqs = boto3.client('sqs')

def convert_to_camel_case(match):
    """
    Helper function to convert JSON keys to camelCase.
    
    :param match: The regex match object
    :return: The camelCase string
    """
    s = match.group(1)  # Get the content between the quotes
    return f'"{s[0].lower() + s[1:]}"'  # Convert and wrap in single quotes

def replace_json_pattern(text):
    """
    Replace JSON keys with camelCase format.
    
    :param text: The JSON string
    :return: The JSON string with keys in camelCase
    """
    pattern = r'"([A-Z][a-zA-Z0-9]*?)"'
    return re.sub(pattern, convert_to_camel_case, text)

def lambda_handler(event, context):
    """
    Lambda function to process the Rekognition person tracking results.
    
    :param event: The event dict containing the SNS message
    :param context: The context object for the Lambda function
    :return: A dictionary containing the status code and response
    """
    try:
        # Extract the message from the SNS event
        message_body = json.loads(event['Records'][0]['Sns']['Message'])
        
        if 'JobId' not in message_body:
            logger.error("JobId not found in the message")
            return {
                'statusCode': 400,
                'body': json.dumps('JobId not found in the message')
            }

        job_id = message_body['JobId']
        source_bucket = message_body['Video']['S3Bucket']
        source_key = message_body['Video']['S3ObjectName']
        
        logger.info(f"Processing job: {job_id}, video: {source_key} from bucket: {source_bucket}")

        # Extract video name without path
        video_name = os.path.basename(source_key)
        video_name_without_ext = os.path.splitext(video_name)[0]
        
        # Define destination paths
        dest_folder = f"result/{video_name_without_ext}"
        dest_video_key = f"{dest_folder}.mp4"
        dest_json_key = f"{dest_folder}.json"

        # Get person tracking results
        response = rekognition.get_person_tracking(JobId=job_id, SortBy='TIMESTAMP', MaxResults=1000)
        
        return_json = replace_json_pattern(json.dumps(response['Persons']))
        
        # Save results to JSON file in S3
        s3.put_object(
            Bucket=S3_BUCKET_NAME,
            Key=dest_json_key,
            Body=return_json.encode('utf-8')
        )
        logger.info(f"JSON results saved to: {dest_json_key}")

        # Copy video from source to destination
        logger.info(f"Video copied to: {dest_video_key}")

        # Delete the original video
        s3.delete_object(
            Bucket=source_bucket,
            Key=source_key
        )
        logger.info(f"Original video deleted: {source_key}")

        # Post message to SQS
        sqs_message = {
            'video': dest_video_key,
            'json': dest_json_key
        }
        
        sqs.send_message(
            QueueUrl=SQS_QUEUE_URL,
            MessageBody=json.dumps(sqs_message)
        )
        logger.info("Message sent to SQS queue")

        return {
            'statusCode': 200,
            'body': json.dumps('Success!')
        }

    except ClientError as e:
        logger.error(f"An error occurred with an AWS client: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
    except Exception as e:
        logger.error(f"An unexpected error occurred: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': 'An unexpected error occurred'})
        }
