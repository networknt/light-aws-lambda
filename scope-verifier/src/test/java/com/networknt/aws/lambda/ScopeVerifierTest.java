package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.utility.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScopeVerifierTest {
    ObjectMapper objectMapper = new ObjectMapper();
    @Test
    public void testSuccessRequest() {
        ScopeVerifier verifier = new ScopeVerifier();
        String json = "{\"resource\":\"/hello\",\"path\":\"/hello\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer 112\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"wievfduueb.execute-api.us-east-1.amazonaws.com\",\"Postman-Token\":\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\",\"X-Amzn-Trace-Id\":\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.138\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\",\"X-Scope-Token\":\"Bearer 222\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer 112\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"wievfduueb.execute-api.us-east-1.amazonaws.com\"],\"Postman-Token\":[\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.138\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 222\"]},\"queryStringParameters\":{},\"multiValueQueryStringParameters\":{},\"pathParameters\":{},\"stageVariables\":{},\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"9b3qv0\",\"requestId\":\"2ca51c69-3090-407b-bf87-9b86185fadb0\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/hello\",\"httpMethod\":\"GET\",\"apiId\":\"wievfduueb\",\"path\":\"/Prod/hello\",\"authorizer\":null},\"body\":null,\"isBase64Encoded\":null}";
        /*
        try {
            APIGatewayProxyRequestEvent request = objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);
            result = verifier.verifyScope(request);
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));

        } catch (Exception e) {
            e.printStackTrace();
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Traceability-Id", "123");
        headers.put("X-Correlation-Id", "456");
        event.setHeaders(headers);
        */
    }

    @Test
    public void testFailureRequest() {

    }

    @Test
    public void testStringArray() {
        List<String> strings = new ArrayList<>();
        strings.add("ABC");
        strings.add("DEF");
        strings.add("ZYZ");

        String s = StringUtils.join(strings, ',');
        System.out.println(s);
        String[] scopes = StringUtils.split(s, ',');
        System.out.println(scopes);
    }
}
