# MoneyGrab Lambda Functions

This repository hosts all the AWS Lambda functions used in the **MoneyGrab Application**. It is a monorepo containing multiple Lambda functions organized into their respective directories.

## Lambda Functions

- **auth-lambda**  
  Handles authentication and authorization for the MoneyGrab application, including JWT token validation and user identity management.

- **fxupload-lambda**  
  Responsible for uploading and processing foreign exchange (FX) feed data. It ingests FX rate feeds and stores or processes them as needed.

- **rate-lambda**  
  Performs exchange rate computations using various calculation strategies. It calculates computed rates based on input data and business logic.


## Lambda Execution Role

The Lambda functions use the IAM role named **lambda-jwt-authorizer-role**. This role has the following AWS managed policy attached:

- `AWSLambdaBasicExecutionRole`

This policy provides basic Lambda execution permissions such as writing logs to CloudWatch.


## How to Compile

To compile the code for a specific Lambda function, use the following Maven command. For example, to build the `auth-lambda`:

```bash
mvn clean package -pl auth-lambda

```

