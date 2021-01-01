package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LambdaFunctionHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandlerTest.class);

    @Test
    public void testAPIGatewayProxyRequestEvent() throws Exception {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setHttpMethod("get");
        requestEvent.setPath("/pets");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        requestEvent.setHeaders(headers);
        Map<String, String> pathParameters = new HashMap<>();
        requestEvent.setPathParameters(pathParameters);
        Map<String, String> queryStringParameters = new HashMap<>();
        requestEvent.setQueryStringParameters(queryStringParameters);
        requestEvent.setBody(null);
        System.out.println(JsonMapper.objectMapper.writeValueAsString(requestEvent));

    }
}
