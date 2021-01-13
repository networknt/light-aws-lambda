package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LightLambdaInterceptor {
    private static ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(LightLambdaInterceptor.class);
    public APIGatewayProxyRequestEvent interceptRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            if(logger.isTraceEnabled()) logger.trace(objectMapper.writeValueAsString(input));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return input;
    }

    public APIGatewayProxyResponseEvent interceptResponse(APIGatewayProxyResponseEvent response) {
        try {
            if(logger.isTraceEnabled()) logger.trace(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
