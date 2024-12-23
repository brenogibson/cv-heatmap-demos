# Guidance for Retail Video Analytics on AWS

## Table of Contents

1. [Overview](#overview)
    - [Cost](#cost)
2. [Prerequisites](#prerequisites)
    - [Operating System](#operating-system)
3. [Deployment Steps](#deployment-steps)
4. [Deployment Validation](#deployment-validation)
5. [Running the Guidance](#running-the-guidance)
6. [Next Steps](#next-steps)
7. [Cleanup](#cleanup)
8. [FAQ, Known Issues, Additional Considerations, and Limitations](#faq-known-issues-additional-considerations-and-limitations)
9. [Notices](#notices)
10. [Authors](#authors)

## Overview 

Retail Video Analytics uses existing retail store cameras to generate customer analytics through computer vision. Retailers across various segments recognize the benefits of Video Analytics to understand customer preferences, identify bottlenecks, and improve the in-store experience. This solution helps retailers derive powerful insights on in-store shopper behavior, similar to the detailed metrics they get from their online stores.

The solution collects video feeds from existing cameras in physical retail locations, performs inference either in the AWS Cloud or at the Edge, and anonymizes the inference data to create actionable intelligence about shopper activity and patterns. This allows retailers to:

- Assign associates to high-traffic areas to provide better assistance
- Facilitate interactions during high-value product purchases
- Manage checkout queues more effectively
- Optimize merchandising strategies and layouts

Retailers can use these insights to identify peak shopping times and optimize staffing levels and task assignments. Additionally, Video Analytics can drive innovative processes, such as providing attribution for retail media networks or generating evidence to support trade fund negotiations.

![Architecture](./assets/images/Architecture.png)

### Cost 

You are responsible for the cost of the AWS services used while running this Guidance. As of December 2024, the cost for running this Guidance with the default settings in the US East (N. Virginia) Region is approximately $76 per month for processing 56 hours (8 hours per day, 7 days) of video.

We recommend creating a [Budget](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html) through [AWS Cost Explorer](https://aws.amazon.com/aws-cost-management/aws-cost-explorer/) to help manage costs. Prices are subject to change. For full details, refer to the pricing webpage for each AWS service used in this Guidance.

### Sample Cost Table 

The following table provides a sample cost breakdown for deploying this Guidance with default parameters in the US East (N. Virginia) Region for one month:

| AWS Service  | Dimensions | Cost (USD) |
|-------------|------------|-------------|
| Amazon S3 | 84 GB of data storage per month | $1.93 |
| Amazon EC2 | 8 hours × 7 days per month | $74.10 |
| Amazon SQS | 1,000 REST API calls per month | $0.01 |
| **Total** | | **$76.04** |

*Note: Calculations assume one week of security camera video processing (7 days × 8 hours per day) and an average of 1.5 GB storage per hour of video.*

## Prerequisites

### AWS Requirements
To deploy this solution, you need:
- An AWS account
- AWS CloudFormation access to the following services:
  - Amazon S3
  - Amazon EC2
  - Amazon SQS

### Local Development Requirements
To deploy the front-end application locally, you need:
- Linux or MacOS operating system
- Java Runtime Environment
- AWS CLI 2.0 configured with credentials for S3 bucket read access

### Operating System Requirements

#### Backend (Video Processing)
- **Recommended Environment:**
  - EC2 instance with **Ubuntu 22.04 - Deep Learning OSS Nvidia Driver AMI GPU PyTorch 2.5**
  - GPU acceleration for optimal performance

#### Frontend (Visualization Application)
- **System Requirements:**
  - Operating System: Linux or MacOS
  - Java Runtime Environment (JRE) 8 or later
  - Web browser (Chrome, Firefox, or Safari recommended)

- **Deployment Options:**
  1. Local Deployment:
     - AWS CLI installed and configured
     - S3 bucket read permissions
  
  2. EC2 Deployment:
     - Instance Profile with S3 read permissions
     - Public IP or DNS for access
     - Optional: CloudFront distribution for content delivery

Note: The backend and frontend can be deployed independently. The backend processes videos and stores results in S3, while the frontend reads from S3 to visualize the results.

### Supported Regions 

This Guidance currently supports only the us-east-1 (N.Virginia) region.

## Deployment Steps 

1. **CloudFormation Deployment**
   - Clone this repository
   - Navigate to the AWS CloudFormation console in us-east-1 region
   - Choose [Create Stack](https://us-east-1.console.aws.amazon.com/cloudformation/home?region=us-east-1#/stacks/create)
   - Upload the template file (`./deployment/Retail-Video-Analytics.cfn.yml`)
   - Choose a name for the stack and proceed to create it

   The template deploys:
   - Amazon S3 bucket for video file storage
   - Amazon SQS queue for new object notifications
   - Amazon EC2 instance for video processing using Ultralytics YOLOv9 and Bytetrack

2. **Web Interface Setup**
   - Install AWS CLI and configure credentials with S3 bucket read permissions
   - For EC2 deployment: Configure Instance Profile with appropriate S3 read permissions
   - Install [Java](https://www.java.com/en/download/)
   - Install [Maven](https://maven.apache.org/install.html)
      - If using an Ubuntu environment: `sudo apt install default-jre maven -y`
   - In terminal, navigate to `./source/webapp`, then run
   ```bash
   sudo chmod +x setup-bucket.sh 
   sudo chmod +x host-start.sh 
   bash setup-bucket.sh <bucket-name>
   bash host-start.sh
   ```
   Depending on your permissions, it could be necessary to execute it as sudo, such as: 

   `sudo bash setup-bucket.sh video-analytics-bucket-1234567890`

3. **Access the Application on your Browser**
   - Local deployment: http://localhost:8080
   - If deployed in EC2: Use the instance's public IP or DNS address

## Deployment Validation

1. **CloudFormation Verification**
   - Open CloudFormation console
   - Check the status of the RetailVideoAnalytics stack
   - Verify all resources (S3, SQS, EC2) in the outputs section

## Running the Guidance

1. Upload a sample store video to the S3 input folder
2. Wait for processing (typically a few minutes)
3. Check the S3 output folder for:
   - JSON file with tracking data
   - Processed video file
4. Access the UI to view:
   - People tracking overlay
   - Heat maps
   - Analytics visualization

![Retail Video Analysis Output 1](./assets/images/RetailVideImage-1.png)
![Retail Video Analysis Output 2](./assets/images/RetailVideImage-2.png)
![Retail Video Analysis Output 3](./assets/images/RetailVideImage-3.png)

## Next Steps

Consider these enhancements:
- Deploy the UI on EC2 with Amazon CloudFront as CDN
- Implement authentication using Amazon Cognito
- Scale the solution for larger video processing needs

## Cleanup

1. Empty the S3 bucket:
   ```bash
   aws s3 rm s3://bucket-name --recursive
   ```
2. Delete the RetailVideoAnalytics CloudFormation stack

## FAQ and Known Issues

### Known Issues
- **Video Length Limitation**: Videos longer than 10 minutes are not supported
  - Solution is optimized for 10-minute segments
  - Longer videos may exhaust compute resources
  - Consider using larger instances or video splitting for longer content

For feedback or questions, please use the repository's Issues tab.

## Notices

*Customers are responsible for making their own independent assessment of the information in this Guidance. This Guidance: (a) is for informational purposes only, (b) represents AWS current product offerings and practices, which are subject to change without notice, and (c) does not create any commitments or assurances from AWS and its affiliates, suppliers or licensors. AWS products or services are provided "as is" without warranties, representations, or conditions of any kind, whether express or implied. AWS responsibilities and liabilities to its customers are controlled by AWS agreements, and this Guidance is not part of, nor does it modify, any agreement between AWS and its customers.*

## Authors

- Sriharsh Adari (saadari@)
- Breno Gibson Silva (brenos@)
