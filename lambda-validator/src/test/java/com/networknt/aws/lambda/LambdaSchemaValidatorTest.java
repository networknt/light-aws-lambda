package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class LambdaSchemaValidatorTest {
    ObjectMapper objectMapper = new ObjectMapper();
    private final LambdaSchemaValidator validator = new LambdaSchemaValidator();
    APIGatewayProxyRequestEvent requestEvent;


    @BeforeEach
    public  void setUp() {
        requestEvent = new APIGatewayProxyRequestEvent();
    }

    @Test
    public void testSuccessRequest() {
        String json = "{\"resource\":\"/v1/pets\",\"path\":\"/v1/pets\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"tkr4v5s580.execute-api.us-east-2.amazonaws.com\",\"Postman-Token\":\"fbe9741b-a622-441d-a51d-d6978950964c\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 c0216388ff1632eb6c4704890b01eee5.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"0sImSLnVrCPzegFEkK-01kv_t2jIFGbjbR5iKaBuO_nd36GKInbqig==\",\"X-Amzn-Trace-Id\":\"Root=1-5fdd0b4a-3d8d07e21644aa6c17dd325e\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.92\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\",\"X-Scope-Token\":\"Bearer 456\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"tkr4v5s580.execute-api.us-east-2.amazonaws.com\"],\"Postman-Token\":[\"fbe9741b-a622-441d-a51d-d6978950964c\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 c0216388ff1632eb6c4704890b01eee5.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"0sImSLnVrCPzegFEkK-01kv_t2jIFGbjbR5iKaBuO_nd36GKInbqig==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fdd0b4a-3d8d07e21644aa6c17dd325e\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.92\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 456\"]},\"queryStringParameters\":null,\"multiValueQueryStringParameters\":null,\"pathParameters\":null,\"stageVariables\":null,\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"zi3e0o\",\"requestId\":\"0e194433-a8d1-4239-9c81-4935e80b577f\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/v1/pets\",\"httpMethod\":\"GET\",\"apiId\":\"tkr4v5s580\",\"path\":\"/Prod/v1/pets\",\"authorizer\":{\"primary_scopes\":\"filenet.document.read filenet.document.write\",\"principalId\":\"0oa41p1u0lR4h1LDM1d6\",\"integrationLatency\":8510,\"client_id\":\"0oa41p1u0lR4h1LDM1d6\"}},\"body\":null,\"isBase64Encoded\":false}";
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

    /**
     * This might not be a valid test case. You actually cannot set path parameter with different names.
     */
    @Test
    @Disabled
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
