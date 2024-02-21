#!/bin/sh

if [ -d "target" ]; then
  rm -r "target/*sources.jar"
  rm -r "target/*javadoc.jar"

  awslocal lambda create-function \
    --function-name "local-lambda" \
    --runtime "java11" \
    --handler "com.networknt.aws.LocalLambdaFunction::handleRequest" \
    --zip-file fileb://target/local-lambda-2.1.32-SNAPSHOT.jar \
    --role arn:aws:iam::000000000000:role/lambda-role
fi
