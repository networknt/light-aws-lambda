package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class ConfigurationTest {
    ObjectMapper objectMapper = new ObjectMapper();
    @Test
    @Disabled
    public void thatConfigurationValuesAreLoaded() throws Exception {
        String json = "{\"resource\":\"/hello\",\"path\":\"/hello\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer 123\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"wievfduueb.execute-api.us-east-1.amazonaws.com\",\"Postman-Token\":\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\",\"X-Amzn-Trace-Id\":\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.138\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\",\"X-Scope-Token\":\"Bearer 456\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer 112\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"wievfduueb.execute-api.us-east-1.amazonaws.com\"],\"Postman-Token\":[\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.138\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 222\"]},\"queryStringParameters\":{},\"multiValueQueryStringParameters\":{},\"pathParameters\":{},\"stageVariables\":{},\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"9b3qv0\",\"requestId\":\"2ca51c69-3090-407b-bf87-9b86185fadb0\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/hello\",\"httpMethod\":\"GET\",\"apiId\":\"wievfduueb\",\"path\":\"/Prod/hello\",\"authorizer\":null},\"body\":null,\"isBase64Encoded\":null}";
        try {
            APIGatewayProxyRequestEvent request = objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);

            Configuration configuration = new Configuration();
            Map<String, Object> envConfig  = configuration.getConfigMap(request.getRequestContext().getStage());
            Assertions.assertNotNull(envConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
