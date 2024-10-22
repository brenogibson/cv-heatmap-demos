import json
import boto3
import urllib.parse
import logging
import os
from botocore.exceptions import ClientError

# Set up logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Initialize the Rekognition client
rekognition = boto3.client('rekognition')

def lambda_handler(event, context):
    """
    Lambda function to start person tracking on a video uploaded to S3.
    
    :param event: The event dict containing details about the S3 upload
    :param context: The context object for the Lambda function
    :return: A dictionary containing the status code and response from Rekognition
    """
    try:
        # Extract bucket and key information from the event
        key = urllib.parse.unquote_plus(event['Records'][0]['s3']['object']['key'])
        bucket_name = event['Records'][0]['s3']['bucket']['name']
        
        logger.info(f"Processing video: {key} from bucket: {bucket_name}")

        # Fetch SNS topic ARN and IAM role ARN from environment variables
        sns_topic_arn = os.environ.get('SNS_TOPIC_ARN')
        role_arn = os.environ.get('REKOGNITION_ROLE_ARN')

        if not sns_topic_arn or not role_arn:
            raise ValueError("SNS_TOPIC_ARN or REKOGNITION_ROLE_ARN environment variable is missing")

        # Start person tracking
        response = rekognition.start_person_tracking(
            Video={
                'S3Object': {
                    'Bucket': bucket_name, 
                    'Name': key
                }
            },
            ClientRequestToken=key.split('.')[0].split('/')[-1],  # Use the filename as the request token
            NotificationChannel={
                'SNSTopicArn': sns_topic_arn,
                'RoleArn': role_arn
            },
            JobTag='people-pathing'
        )
        
        logger.info(f"Person tracking job started successfully. Job ID: {response['JobId']}")

        return {
            'statusCode': 200,
            'body': json.dumps(response)
        }

    except ClientError as e:
        logger.error(f"An error occurred with Rekognition client: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps({'error': str(e)})
        }
    except ValueError as e:
        logger.error(f"Environment variable error: {e}")
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
