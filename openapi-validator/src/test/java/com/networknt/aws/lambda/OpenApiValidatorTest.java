package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mservicetech.openapi.common.RequestEntity;
import com.mservicetech.openapi.common.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class OpenApiValidatorTest {
    ObjectMapper objectMapper = new ObjectMapper();
    private OpenApiValidator validator = new OpenApiValidator();
    APIGatewayProxyRequestEvent requestEvent;


    @BeforeEach
    public  void setUp() {
        requestEvent = new APIGatewayProxyRequestEvent();
    }

    @Test
    public void testSuccessRequest() {
        String json = "{\"resource\":\"/v1/pets\",\"path\":\"/v1/pets\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer eyJraWQiOiJESlFLUDJISlZ2YTQweVN1UnZLaFRMaG01YjhPRnRYNWdOQlFFVGFJSEdRIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULnFLUlRwbmk5QjBaM042Z2U3WTRaRXA0T0VwaTMyZDVVOWk0WmQ0WnZmV0UiLCJpc3MiOiJodHRwczovL3N1bmxpZmVhcGkub2t0YXByZXZpZXcuY29tL29hdXRoMi9hdXM0MWJ3cDJCYWZBQnA3aDFkNiIsImF1ZCI6ImRldi5jYW5hZGEuY29ycG9yYXRlLnN1bmxpZmVhcGlzLm9rdGFwcmV2aWV3LmNvbSIsImlhdCI6MTYwODEzMzY0NiwiZXhwIjoxNjA4MjIwMDQ2LCJjaWQiOiIwb2E0MXAxdTBsUjRoMUxETTFkNiIsInNjcCI6WyJmaWxlbmV0LmRvY3VtZW50LnJlYWQiLCJmaWxlbmV0LmRvY3VtZW50LndyaXRlIl0sInN1YiI6IjBvYTQxcDF1MGxSNGgxTERNMWQ2In0.OAnrgiPtglVFLyoDKKj1DQTOj6zT75aOy_1OG-Gpj2zAk9CMq0sV5ZRsljJhzPyo5BGv-hs981efSr6rU9ChAqiAsXvb1gMwmXqk7t93yrqB22Gms1Um-u5Epdr45COFCQJK5WUtLVTH7U1f8VbhCNcNndRSO2_UTtaDJsy_L6SaV-ybk2hoih6CvFRw6rBA40BgkGxVLaI2CtAv5R6vnKRunDePRQngY3iXvyCni_y7w-tF6dzv_AcFtLnLB5lUvXa7_Dh89pTgQ1Bzj2Yv9tLo14mtpbTPIcHanV9rm5MdZp_H2DNnOLidthC8TUzBbtn9oOgNbqw9EzQAw1or6w\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"tkr4v5s580.execute-api.us-east-2.amazonaws.com\",\"Postman-Token\":\"fbe9741b-a622-441d-a51d-d6978950964c\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 c0216388ff1632eb6c4704890b01eee5.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"0sImSLnVrCPzegFEkK-01kv_t2jIFGbjbR5iKaBuO_nd36GKInbqig==\",\"X-Amzn-Trace-Id\":\"Root=1-5fdd0b4a-3d8d07e21644aa6c17dd325e\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.92\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\",\"X-Scope-Token\":\"Bearer 456\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer eyJraWQiOiJESlFLUDJISlZ2YTQweVN1UnZLaFRMaG01YjhPRnRYNWdOQlFFVGFJSEdRIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULnFLUlRwbmk5QjBaM042Z2U3WTRaRXA0T0VwaTMyZDVVOWk0WmQ0WnZmV0UiLCJpc3MiOiJodHRwczovL3N1bmxpZmVhcGkub2t0YXByZXZpZXcuY29tL29hdXRoMi9hdXM0MWJ3cDJCYWZBQnA3aDFkNiIsImF1ZCI6ImRldi5jYW5hZGEuY29ycG9yYXRlLnN1bmxpZmVhcGlzLm9rdGFwcmV2aWV3LmNvbSIsImlhdCI6MTYwODEzMzY0NiwiZXhwIjoxNjA4MjIwMDQ2LCJjaWQiOiIwb2E0MXAxdTBsUjRoMUxETTFkNiIsInNjcCI6WyJmaWxlbmV0LmRvY3VtZW50LnJlYWQiLCJmaWxlbmV0LmRvY3VtZW50LndyaXRlIl0sInN1YiI6IjBvYTQxcDF1MGxSNGgxTERNMWQ2In0.OAnrgiPtglVFLyoDKKj1DQTOj6zT75aOy_1OG-Gpj2zAk9CMq0sV5ZRsljJhzPyo5BGv-hs981efSr6rU9ChAqiAsXvb1gMwmXqk7t93yrqB22Gms1Um-u5Epdr45COFCQJK5WUtLVTH7U1f8VbhCNcNndRSO2_UTtaDJsy_L6SaV-ybk2hoih6CvFRw6rBA40BgkGxVLaI2CtAv5R6vnKRunDePRQngY3iXvyCni_y7w-tF6dzv_AcFtLnLB5lUvXa7_Dh89pTgQ1Bzj2Yv9tLo14mtpbTPIcHanV9rm5MdZp_H2DNnOLidthC8TUzBbtn9oOgNbqw9EzQAw1or6w\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"tkr4v5s580.execute-api.us-east-2.amazonaws.com\"],\"Postman-Token\":[\"fbe9741b-a622-441d-a51d-d6978950964c\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 c0216388ff1632eb6c4704890b01eee5.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"0sImSLnVrCPzegFEkK-01kv_t2jIFGbjbR5iKaBuO_nd36GKInbqig==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fdd0b4a-3d8d07e21644aa6c17dd325e\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.92\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 456\"]},\"queryStringParameters\":null,\"multiValueQueryStringParameters\":null,\"pathParameters\":null,\"stageVariables\":null,\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"zi3e0o\",\"requestId\":\"0e194433-a8d1-4239-9c81-4935e80b577f\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/v1/pets\",\"httpMethod\":\"GET\",\"apiId\":\"tkr4v5s580\",\"path\":\"/Prod/v1/pets\",\"authorizer\":{\"primary_scopes\":\"filenet.document.read filenet.document.write\",\"principalId\":\"0oa41p1u0lR4h1LDM1d6\",\"integrationLatency\":8510,\"client_id\":\"0oa41p1u0lR4h1LDM1d6\"}},\"body\":null,\"isBase64Encoded\":false}";
        try {
            APIGatewayProxyRequestEvent request = objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);
            APIGatewayProxyResponseEvent response = validator.validateRequest(request);
            Assertions.assertNull(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInvalidBody() {
        String req = "{\"name\":\"Cat\", \"tag\":\"new tag\"}";
        requestEvent.setPath("pets");
        requestEvent.setHttpMethod("post");
        requestEvent.setBody(req);
        APIGatewayProxyResponseEvent response = validator.validateRequest(requestEvent);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getStatusCode(), 400);  //The invalid request
    }

    @Test
    public void testValidPathParameter() {
        requestEvent.setPath("/pets/{petId}");
        requestEvent.setHttpMethod("get");
        Map<String, String> pathMap = new HashMap<>();
        pathMap.put("petId", "1122");
        requestEvent.setPathParameters(pathMap);
        APIGatewayProxyResponseEvent response = validator.validateRequest(requestEvent);
        Assertions.assertNull(response);
    }

    @Test
    public void testInvalidPathParameter() {

        requestEvent.setPath("/pets/{petId}");
        requestEvent.setHttpMethod("get");
        Map<String, String> pathMap = new HashMap<>();
        pathMap.put("petId2", "1122");
        requestEvent.setPathParameters(pathMap);
        APIGatewayProxyResponseEvent response = validator.validateRequest(requestEvent);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getStatusCode(), 400);
    }

    @Test
    public void testValidQueryParameter() {
        requestEvent.setPath("/pets");
        requestEvent.setHttpMethod("get");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("includeCode", "true");
        requestEvent.setQueryStringParameters(queryMap);
        APIGatewayProxyResponseEvent response = validator.validateRequest(requestEvent);
        Assertions.assertNull(response);
    }

    @Test
    public void testInValidQueryParameter() {
        requestEvent.setPath("/pets");
        requestEvent.setHttpMethod("get");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("includeCode", "true1");
        requestEvent.setQueryStringParameters(queryMap);
        APIGatewayProxyResponseEvent response = validator.validateRequest(requestEvent);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getStatusCode(), 400);
    }

    @Test
    public void testInValidQueryParameter2() {
        requestEvent.setPath("/pets");
        requestEvent.setHttpMethod("get");
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("limit", "abbb");
        requestEvent.setQueryStringParameters(queryMap);
        APIGatewayProxyResponseEvent response = validator.validateRequest(requestEvent);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(response.getStatusCode(), 400);
    }
}
