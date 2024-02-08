# light-aws-lambda
Aws lambda authorizer, middleware handlers and extensions for cross-cutting concerns

### authorizer

The authorizer is a lambda function and it will be configured on the AWS API Gateway to be invoked as a custom authorizer.


### middleware handlers

Middleware handlers are called by the framework before the Lambda function handlers are called to do the following.

##### JWT scope verifier

The custome authorizer on the API Gateway will enrich the request context with client_id/cid, user_id/uid (authorization flow), primary scopes, secondary scopes. The authorizer doesn't have the knowledge of OpenAPI specification on the API Gateway so the scope verification will be done on this middleware handler based on the openapi.yml specification.


##### Schema Validatior

After the JWT scope verifier, the same openapi.yaml will be leveraged to validate the request with json-schema-validator.

### lambda extensions


##### Logger

This is a Lambda extersion that will collect the logs of the Lambda function and send to Splunk or Elasticsearch.

##### Metrics

This is a Lambda extension that will collect the metrics of the Lambda function and send to a time series database.
