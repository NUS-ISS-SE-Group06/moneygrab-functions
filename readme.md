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
mvn clean package -pl <lambda-module>

mvn clean package -pl auth-lambda
mvn clean package -pl FXFileUpload-lambda
mvn clean package -pl rate-lambda

```
## How to Local Build with AWS SAM

To build the project locally and generate the .aws-sam directory for local testing and deployment:

```bash
# Navigate to the project directory
# cd /moneygrab-functions
# sam build <Resources_Name>   

sam build

````

## Output Structure

After a successful build, your folder structure should look like this:

```pgsql
moneygrab-functions/
├── rate-lambda/
├── auth-lambda/
├── template.yaml
├── .aws-sam/
│   └── build/
│       ├── RateLambdaFunction/
│       └── template.yaml
```

## How to Invoke Lambda Locally with SAM

After successfully building your Lambda using the AWS SAM CLI, you can test it locally using the sam local invoke command. This allows you to simulate how your Lambda function behaves in the AWS environment — without deploying it to the cloud.

```bash
# Go to Project Root Directory
# ~/moneygrab-functions/

# sam local invoke <Resources-Name-In-Template-yml> --template .aws-sam/build/template.yaml --event /resources/event.json

sam local invoke RateLambdaFunction --template .aws-sam/build/template.yaml --event resources/sample-rate-lambda-event.json 

```

## Sample Test Data (event.json)
```pgsql
moneygrab-functions/
├── resources/
│   └── sample-rate-lambda-event.json
```