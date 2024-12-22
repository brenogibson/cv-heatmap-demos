# Guidance for Retail Video Analytics on AWS

## Table of Contents
List the top-level sections of the README template, along with a hyperlink to the specific section.

### Required

1. [Overview](#overview)
    - [Cost](#cost)
2. [Prerequisites](#prerequisites)
    - [Operating System](#operating-system)
3. [Deployment Steps](#deployment-steps)
4. [Deployment Validation](#deployment-validation)
5. [Running the Guidance](#running-the-guidance)
6. [Next Steps](#next-steps)
7. [Cleanup](#cleanup)

***Optional***

8. [FAQ, known issues, additional considerations, and limitations](#faq-known-issues-additional-considerations-and-limitations)
9. [Notices](#notices)
10. [Authors](#authors)

## Overview 

Retail Video Analytics uses video from existing retail store cameras to generate customer analytics through computer vision technology. Retailers across various segments recognize the benefits of Video Analytics to understand customer preferences, identify bottlenecks, and improve the in-store experience. Video Analytics is a machine learning area designed to help retailers derive powerful insights on in-store shopper behavior, similar to the detailed metrics and analytics they get from their online stores. Video Analytics collects video feeds from existing cameras in physical retail locations, performs inference either in the AWS Cloud or at the Edge, and anonymizes the inference data to create actionable intelligence about shopper activity and patterns. This allows retailers to improve the overall shopper experience, such as:

        -Assigning associates to high-traffic areas to provide better assistance
        -Facilitating interactions during high-value product purchases
        -Managing checkout queues more effectively
        -Optimizing merchandising strategies and layouts

Retailers can also use these insights to identify peak shopping times and optimize staffing levels and task assignments accordingly. Additionally, Video Analytics can drive innovative new processes, such as providing attribution for retail media networks or generating evidence to support trade fund negotiations.  As a service, Video Analytics makes it easy for retailers to implement and start deriving value quickly, without the need for specialized machine learning expertise. 


Please find below architecture diagram image, as well as the steps explaining the high-level overview and flow of the architecture. 
    ![Architecture](./assets/images/Architecture.png)

### Cost 

This section is for a high-level cost estimate. Think of a likely straightforward scenario with reasonable assumptions based on the problem the Guidance is trying to solve. Provide an in-depth cost breakdown table in this section below ( you should use AWS Pricing Calculator to generate cost breakdown ).

_You are responsible for the cost of the AWS services used while running this Guidance. As of November 2024, the cost for running this Guidance with the default settings in the US East (N. Virginia)) > is approximately $125 per month for processing 360 mins of video.


_We recommend creating a [Budget](https://docs.aws.amazon.com/cost-management/latest/userguide/budgets-managing-costs.html) through [AWS Cost Explorer](https://aws.amazon.com/aws-cost-management/aws-cost-explorer/) to help manage costs. Prices are subject to change. For full details, refer to the pricing webpage for each AWS service used in this Guidance._

### Sample Cost Table 

**Note : Once you have created a sample cost table using AWS Pricing Calculator, copy the cost breakdown to below table and upload a PDF of the cost estimation on BuilderSpace. Do not add the link to the pricing calculator in the ReadMe.**

The following table provides a sample cost breakdown for deploying this Guidance with the default parameters in the US East (N. Virginia) Region for one month.

| AWS service  | Dimensions | Cost [USD] |
| ----------- | ------------ | ------------ |
| Amazon S3 | 84 GB of data pricing per month | $ 1.93 / month |
| Amazon EC2 | 8 hours of EC2 usage per day, for 7 days a month | $ 74.10 / month |
| Amazon SQS | 1000 REST API calls per month  | $ 0.01 / month |
| **Total** |   | **$ 76.04 / month** |

Note that it considers that for each month was considered 7 days x 8 hours per day. One week of security camera video processing should give retailers insights about customers' habits. It was assumed an average of 1.5 GB of storage per hour of video.

## prerequisites

To deploy this solution, you need to have an AWS account and give AWS CloudFormation access to the following main services:
- Amazon S3
- Amazon EC2
- Amazon SQS

To deploy front-end application locally, you'll need:
- Linux or MacOS
- Java
- AWS CLI 2.0 with credentials that give read access to solution's S3 bucket

### Operating System 

These deployment instructions are optimized to best work on **Ubuntu 22.04 - Deep Learning OSS Nvidia Driver AMI GPU PyTorch 2.5** EC2 instances.  Deployment in another OS or instance family may require additional steps.
Source code is intended to run on EC2, but you can run it locally with Python 3.10 or superior, and libraries included in requirements.txt file. Be aware that inference is heavily improved by GPU usage.

For front-end visualizing application, you will need a machine with Linux or MacOS operating system. You can deploy it on EC2 as well.

### Supported Regions 

This Guidance is built for US-east-1 (N.Virginia), the services used in the Guidance do not support different Regions at this point.


## Deployment Steps 

1. Download this CloudFormation template and execute it in US-East-1 (N.Virginia) region. It deploys the following components:

- Amazon S3 bucket to upload video files
- Amazon SQS new object notification queue which is triggered as soon as a new video file is uploaded to input/ folder in the S3 bucket.
- Amazon EC2 instance which process the video file using Ultralytics YOLOv9 and Bytetrack for poeple tracking. The processed video and JSON file with people pathing is placed in output/ folder of Amazon S3 bucket created in Step#1.
2. For the web interface to run locally, you will need to have AWS CLI installed. Configure credentials to dowload objects from the S3 bucket created in Step#1.
2.1. If you  want to run from an EC2 instance, instead of configuring AWS CLI, you should configure Instance Profile with a Role with required S3 permissions.
3. Download and extract RetailVideoAnalytcs-HeatmapUI.zip file which contains the UI that will overlay people pathing JSON on the store video file and generate heat maps.
4. Install [Java](https://www.java.com/en/download/) in your environment, if not installed. 
5. Unzip RetailVideoAnalytcs-HeatmapUI.zip 
5. Navigate to  src>main>java>awsPrototype>metadatas>Constants.java, find DEFAULT_S3_BUCKET_NAME and set it to your bucket name.
6. Open your terminal, from the application root folder and run "bash host-start.sh bucket-name" to start application's server. Remember to update bucket-name with the name of your bucket.
7. Now you can navigate to your browser and run the application using 8080 port. If you're running from a local machine, address should be http://localhost:8080.


## Deployment Validation  

* Open CloudFormation console and verify the status of the template with the name starting with RetailVideoAnalytics.yaml.
* If deployment is successful, you should notice all the resources (S3, SQS and EC2)  in the outputs section of cloud formation console.


## Running the Guidance 

* Now upload a sample Store Video in Amazon S3 Input folder.
* After few minutes you should notice a JSON file and a video file in output folder of S3 bucket.
* Perform Steps outlined in Deployment Steps (4, 5 and 6) to validate UI component.
* you should notice a output video file as the screenshot below whne you access the UI.

  !: /assets/images/RetailVideImage-1.png
  !: /assets/images/RetailVideImage-2.png
  !: /assets/images/RetailVideImage-3.png



## Next Steps 

- As a next step, you can deploy RetailVideoAnalytcs-HeatmapUI.zip on a EC2 instance as well if you intend to share the UI with others, you can utilize Amazon Cloud front with EC2 as origin.
- This UI does not have authentication if you deploy it locally, it is only accessible locally. Once you deploy it on EC2 and use Cloud Front you can consider adding cognito for authentication to enhance security posture.


## Cleanup 

- Delete all the vidoe files and JSON file sin Amazon S3 input and output folders.
- Delete RetailVideoAnalytics cloiud formation template from AWS console.



## FAQ, known issues, additional considerations, and limitations 


**Known issues **

- **Vidoes Longers than 10 mins are not supported** Currently, this gudience has been optimized and tested for 10 minutes-long videos. Longer videos can exhaust compute resources.

For any feedback, questions, or suggestions, please use the issues tab under this repo.


## Notices 

*Customers are responsible for making their own independent assessment of the information in this Guidance. This Guidance: (a) is for informational purposes only, (b) represents AWS current product offerings and practices, which are subject to change without notice, and (c) does not create any commitments or assurances from AWS and its affiliates, suppliers or licensors. AWS products or services are provided “as is” without warranties, representations, or conditions of any kind, whether express or implied. AWS responsibilities and liabilities to its customers are controlled by AWS agreements, and this Guidance is not part of, nor does it modify, any agreement between AWS and its customers.*


## Authors

Sriharsh Adari (saadari@)
Breno Gibson Silva (brenos@) 
