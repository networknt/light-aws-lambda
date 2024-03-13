package com.networknt.aws.lambda.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

/**
 * LambdaHandler is the interface that all Lambda function handlers must implement.
 * It extends the RequestHandler interface from the AWS Lambda Java SDK with some
 * common static methods and constants.
 */
public interface LambdaHandler extends RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

}
