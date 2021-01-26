package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightLambdaInterceptor {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(LightLambdaInterceptor.class);

    public APIGatewayProxyRequestEvent interceptRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LambdaLogger lambdaLogger = context.getLogger();
        try {
            lambdaLogger.log("CONTEXT:" + context);
            lambdaLogger.log("EVENT:" + objectMapper.writeValueAsString(input));
            if(logger.isTraceEnabled()) logger.trace(objectMapper.writeValueAsString(input));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return input;
    }

    public APIGatewayProxyResponseEvent interceptResponse(APIGatewayProxyResponseEvent response) {
        try {
            System.out.println("response:" + response.toString());
            if(logger.isTraceEnabled()) logger.trace(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
