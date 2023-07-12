package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

/**
 * This is the entry point for the middleware Lambda function that is responsible for cross-cutting concerns for the business Lambda
 * function which is called from the is Lambda function once all cross-cutting concerns are addressed. The middleware Lambda function
 * receives the APIGatewayProxyRequestEvent from the API Gateway and returns the APIGatewayProxyResponseEvent to the API Gateway.
 *
 * @author Steve Hu
 */
public class App extends LightRequestHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            if(logger.isDebugEnabled()) logger.debug(objectMapper.writeValueAsString(input));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // intercept the request and apply request cross-cutting concerns
        APIGatewayProxyResponseEvent response = interceptRequest(input, context);
        if(response != null) {
            // the response is from the interceptors and return it directly.
            return interceptResponse(response);
        }
        // call the target business Lambda function with SDK. Apply the response cross-cutting concerns before returning
        // to the AWS API Gateway.
        LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_1) // Replace with your desired region
                .build();
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName("your-function-name") // Replace with the name of the target Lambda function
                .build();
        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
        APIGatewayProxyResponseEvent payload = invokeResponse.payload().asUtf8String();

        // Handle the response payload as needed
        System.out.println("Response payload: " + payload);
        return interceptResponse((input, context));
    }
}
