package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.networknt.aws.lambda.security.AuthPolicy;
import org.junit.jupiter.api.Test;


public class AuthorizerTest {
  ObjectMapper objectMapper = new ObjectMapper();

  public static void setEnv(String key, String value) {
    try {
      Map<String, String> env = System.getenv();
      Class<?> cl = env.getClass();
      Field field = cl.getDeclaredField("m");
      field.setAccessible(true);
      Map<String, String> writableEnv = (Map<String, String>) field.get(env);
      writableEnv.put(key, value);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to set environment variable", e);
    }
  }

  /**
   * Both primary token and scope token are available and valid.
   */
  @Test
  public void successfulResponse() {
    Authorizer authorizer = new Authorizer();
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
    headers.put("X-Scope-Token", "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTkyNjY4ODQyOCwianRpIjoibnZ5dVFpelNYelN5eUlYdGxsdDdDQSIsImlhdCI6MTYxMTMyODQyOCwibmJmIjoxNjExMzI4MzA4LCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjdkNDIzNDgtYzY0Ny00ZWZiLWE1MmQtNGM1Nzg3NDIxZTczIiwic2NwIjpbInByb3h5LnIiLCJwcm94eS53Il19.sMfKXuWF-C3fqs0hl7Z2RRiwteUto4XoXgsXfDk5lCE8Hkg1PSA_fvmx7-ag1Pau-ra4iBbD5VK-epPxQSXfGDozPTuDy1MRR6iSH8uljeGWF8BcwYaIXmDB92-RAf09hl1kqpza6s2ivHpgNjA5H-cK13jy2xDNoM6bbNi8VR5mFphw6FpZWZPegrvCOwgUPRJN8Gs_BfQNOhShMpT9_DjQu_UnTTqraf3qw-jZksBQ2txrbHcpi_qF3iF6SgZlMqA31bG-lEbZ5AIpf0kji58pVD96r7wxIoENzrexc7mEt_S64De_lCJJh2plD86gjmR0wHU2Of4e6jFNEDD3Ng");
    request.setHeaders(headers);
    APIGatewayProxyRequestEvent.ProxyRequestContext context = new APIGatewayProxyRequestEvent.ProxyRequestContext();
    setEnv("AWS_REGION", "us-east-1");
    context.setAccountId("1234567890");
    context.setStage("Prod");
    context.setApiId("gy415nuibc");
    context.setHttpMethod("POST");
    APIGatewayProxyRequestEvent.RequestIdentity identity = new APIGatewayProxyRequestEvent.RequestIdentity();
    identity.setAccountId("1234567890");
    context.setIdentity(identity);
    request.setRequestContext(context);
    AuthPolicy result = authorizer.handleRequest(request, new TestContext());
    try {
      System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * It has a wrong X-Scope-Token and that is why it failed for the JWT verification
   */
  @Test
  public void testDenyEvent() {
    Authorizer authorizer = new Authorizer();
    String json = "{\"resource\":\"/hello\",\"path\":\"/hello\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"wievfduueb.execute-api.us-east-1.amazonaws.com\",\"Postman-Token\":\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\",\"X-Amzn-Trace-Id\":\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.138\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\",\"X-Scope-Token\":\"Bearer 222\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer 112\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"wievfduueb.execute-api.us-east-1.amazonaws.com\"],\"Postman-Token\":[\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.138\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 222\"]},\"queryStringParameters\":{},\"multiValueQueryStringParameters\":{},\"pathParameters\":{},\"stageVariables\":{},\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"9b3qv0\",\"requestId\":\"2ca51c69-3090-407b-bf87-9b86185fadb0\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/hello\",\"httpMethod\":\"GET\",\"apiId\":\"wievfduueb\",\"path\":\"/Prod/hello\",\"authorizer\":null},\"body\":null,\"isBase64Encoded\":null}";
    try {
      APIGatewayProxyRequestEvent request = objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);
      AuthPolicy result = authorizer.handleRequest(request, new TestContext());
      System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This time, there is only one Authorization token available and there is no X-Scope-Token in the header.
   */
  @Test
  public void testAllowEvent() {
    Authorizer authorizer = new Authorizer();
    String json = "{\"resource\":\"/hello\",\"path\":\"/hello\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTkyNjY4ODQyOCwianRpIjoibnZ5dVFpelNYelN5eUlYdGxsdDdDQSIsImlhdCI6MTYxMTMyODQyOCwibmJmIjoxNjExMzI4MzA4LCJ2ZXJzaW9uIjoiMS4wIiwiY2xpZW50X2lkIjoiZjdkNDIzNDgtYzY0Ny00ZWZiLWE1MmQtNGM1Nzg3NDIxZTczIiwic2NwIjpbInByb3h5LnIiLCJwcm94eS53Il19.sMfKXuWF-C3fqs0hl7Z2RRiwteUto4XoXgsXfDk5lCE8Hkg1PSA_fvmx7-ag1Pau-ra4iBbD5VK-epPxQSXfGDozPTuDy1MRR6iSH8uljeGWF8BcwYaIXmDB92-RAf09hl1kqpza6s2ivHpgNjA5H-cK13jy2xDNoM6bbNi8VR5mFphw6FpZWZPegrvCOwgUPRJN8Gs_BfQNOhShMpT9_DjQu_UnTTqraf3qw-jZksBQ2txrbHcpi_qF3iF6SgZlMqA31bG-lEbZ5AIpf0kji58pVD96r7wxIoENzrexc7mEt_S64De_lCJJh2plD86gjmR0wHU2Of4e6jFNEDD3Ng\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"wievfduueb.execute-api.us-east-1.amazonaws.com\",\"Postman-Token\":\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\",\"X-Amzn-Trace-Id\":\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.138\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer 112\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"wievfduueb.execute-api.us-east-1.amazonaws.com\"],\"Postman-Token\":[\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.138\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 222\"]},\"queryStringParameters\":{},\"multiValueQueryStringParameters\":{},\"pathParameters\":{},\"stageVariables\":{},\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"9b3qv0\",\"requestId\":\"2ca51c69-3090-407b-bf87-9b86185fadb0\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/hello\",\"httpMethod\":\"GET\",\"apiId\":\"wievfduueb\",\"path\":\"/Prod/hello\",\"authorizer\":null},\"body\":null,\"isBase64Encoded\":null}";
    try {
      APIGatewayProxyRequestEvent request = objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);
      AuthPolicy result = authorizer.handleRequest(request, new TestContext());
      System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
