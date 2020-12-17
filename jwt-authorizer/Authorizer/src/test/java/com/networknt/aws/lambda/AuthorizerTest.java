package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
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

  @Test
  @Disabled
  public void successfulResponse() {
    Authorizer authorizer = new Authorizer();
    APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer eyJraWQiOiJESlFLUDJISlZ2YTQweVN1UnZLaFRMaG01YjhPRnRYNWdOQlFFVGFJSEdRIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULnFLUlRwbmk5QjBaM042Z2U3WTRaRXA0T0VwaTMyZDVVOWk0WmQ0WnZmV0UiLCJpc3MiOiJodHRwczovL3N1bmxpZmVhcGkub2t0YXByZXZpZXcuY29tL29hdXRoMi9hdXM0MWJ3cDJCYWZBQnA3aDFkNiIsImF1ZCI6ImRldi5jYW5hZGEuY29ycG9yYXRlLnN1bmxpZmVhcGlzLm9rdGFwcmV2aWV3LmNvbSIsImlhdCI6MTYwODEzMzY0NiwiZXhwIjoxNjA4MjIwMDQ2LCJjaWQiOiIwb2E0MXAxdTBsUjRoMUxETTFkNiIsInNjcCI6WyJmaWxlbmV0LmRvY3VtZW50LnJlYWQiLCJmaWxlbmV0LmRvY3VtZW50LndyaXRlIl0sInN1YiI6IjBvYTQxcDF1MGxSNGgxTERNMWQ2In0.OAnrgiPtglVFLyoDKKj1DQTOj6zT75aOy_1OG-Gpj2zAk9CMq0sV5ZRsljJhzPyo5BGv-hs981efSr6rU9ChAqiAsXvb1gMwmXqk7t93yrqB22Gms1Um-u5Epdr45COFCQJK5WUtLVTH7U1f8VbhCNcNndRSO2_UTtaDJsy_L6SaV-ybk2hoih6CvFRw6rBA40BgkGxVLaI2CtAv5R6vnKRunDePRQngY3iXvyCni_y7w-tF6dzv_AcFtLnLB5lUvXa7_Dh89pTgQ1Bzj2Yv9tLo14mtpbTPIcHanV9rm5MdZp_H2DNnOLidthC8TUzBbtn9oOgNbqw9EzQAw1or6w");
    headers.put("X-Scope-Token", "Bearer eyJraWQiOiIxMDAiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJ1cm46Y29tOm5ldHdvcmtudDpvYXV0aDI6djEiLCJhdWQiOiJ1cm46Y29tLm5ldHdvcmtudCIsImV4cCI6MTc5MDAzNTcwOSwianRpIjoiSTJnSmdBSHN6NzJEV2JWdUFMdUU2QSIsImlhdCI6MTQ3NDY3NTcwOSwibmJmIjoxNDc0Njc1NTg5LCJ2ZXJzaW9uIjoiMS4wIiwidXNlcl9pZCI6InN0ZXZlIiwidXNlcl90eXBlIjoiRU1QTE9ZRUUiLCJjbGllbnRfaWQiOiJmN2Q0MjM0OC1jNjQ3LTRlZmItYTUyZC00YzU3ODc0MjFlNzIiLCJzY29wZSI6WyJ3cml0ZTpwZXRzIiwicmVhZDpwZXRzIl19.mue6eh70kGS3Nt2BCYz7ViqwO7lh_4JSFwcHYdJMY6VfgKTHhsIGKq2uEDt3zwT56JFAePwAxENMGUTGvgceVneQzyfQsJeVGbqw55E9IfM_uSM-YcHwTfR7eSLExN4pbqzVDI353sSOvXxA98ZtJlUZKgXNE1Ngun3XFORCRIB_eH8B0FY_nT_D1Dq2WJrR-re-fbR6_va95vwoUdCofLRa4IpDfXXx19ZlAtfiVO44nw6CS8O87eGfAm7rCMZIzkWlCOFWjNHnCeRsh7CVdEH34LF-B48beiG5lM7h4N12-EME8_VDefgMjZ8eqs1ICvJMxdIut58oYbdnkwTjkA");
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

  @Test
  public void testAllowEvent() {
    Authorizer authorizer = new Authorizer();
    String json = "{\"resource\":\"/hello\",\"path\":\"/hello\",\"httpMethod\":\"GET\",\"headers\":{\"Accept\":\"*/*\",\"Accept-Encoding\":\"gzip, deflate, br\",\"Authorization\":\"Bearer eyJraWQiOiJESlFLUDJISlZ2YTQweVN1UnZLaFRMaG01YjhPRnRYNWdOQlFFVGFJSEdRIiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULnFLUlRwbmk5QjBaM042Z2U3WTRaRXA0T0VwaTMyZDVVOWk0WmQ0WnZmV0UiLCJpc3MiOiJodHRwczovL3N1bmxpZmVhcGkub2t0YXByZXZpZXcuY29tL29hdXRoMi9hdXM0MWJ3cDJCYWZBQnA3aDFkNiIsImF1ZCI6ImRldi5jYW5hZGEuY29ycG9yYXRlLnN1bmxpZmVhcGlzLm9rdGFwcmV2aWV3LmNvbSIsImlhdCI6MTYwODEzMzY0NiwiZXhwIjoxNjA4MjIwMDQ2LCJjaWQiOiIwb2E0MXAxdTBsUjRoMUxETTFkNiIsInNjcCI6WyJmaWxlbmV0LmRvY3VtZW50LnJlYWQiLCJmaWxlbmV0LmRvY3VtZW50LndyaXRlIl0sInN1YiI6IjBvYTQxcDF1MGxSNGgxTERNMWQ2In0.OAnrgiPtglVFLyoDKKj1DQTOj6zT75aOy_1OG-Gpj2zAk9CMq0sV5ZRsljJhzPyo5BGv-hs981efSr6rU9ChAqiAsXvb1gMwmXqk7t93yrqB22Gms1Um-u5Epdr45COFCQJK5WUtLVTH7U1f8VbhCNcNndRSO2_UTtaDJsy_L6SaV-ybk2hoih6CvFRw6rBA40BgkGxVLaI2CtAv5R6vnKRunDePRQngY3iXvyCni_y7w-tF6dzv_AcFtLnLB5lUvXa7_Dh89pTgQ1Bzj2Yv9tLo14mtpbTPIcHanV9rm5MdZp_H2DNnOLidthC8TUzBbtn9oOgNbqw9EzQAw1or6w\",\"Cache-Control\":\"no-cache\",\"CloudFront-Forwarded-Proto\":\"https\",\"CloudFront-Is-Desktop-Viewer\":\"true\",\"CloudFront-Is-Mobile-Viewer\":\"false\",\"CloudFront-Is-SmartTV-Viewer\":\"false\",\"CloudFront-Is-Tablet-Viewer\":\"false\",\"CloudFront-Viewer-Country\":\"CA\",\"Host\":\"wievfduueb.execute-api.us-east-1.amazonaws.com\",\"Postman-Token\":\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\",\"User-Agent\":\"PostmanRuntime/7.26.8\",\"Via\":\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\",\"X-Amz-Cf-Id\":\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\",\"X-Amzn-Trace-Id\":\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\",\"X-Forwarded-For\":\"64.58.33.5, 64.252.182.138\",\"X-Forwarded-Port\":\"443\",\"X-Forwarded-Proto\":\"https\",\"X-Scope-Token\":\"Bearer 456\"},\"multiValueHeaders\":{\"Accept\":[\"*/*\"],\"Accept-Encoding\":[\"gzip, deflate, br\"],\"Authorization\":[\"Bearer 112\"],\"Cache-Control\":[\"no-cache\"],\"CloudFront-Forwarded-Proto\":[\"https\"],\"CloudFront-Is-Desktop-Viewer\":[\"true\"],\"CloudFront-Is-Mobile-Viewer\":[\"false\"],\"CloudFront-Is-SmartTV-Viewer\":[\"false\"],\"CloudFront-Is-Tablet-Viewer\":[\"false\"],\"CloudFront-Viewer-Country\":[\"CA\"],\"Host\":[\"wievfduueb.execute-api.us-east-1.amazonaws.com\"],\"Postman-Token\":[\"86a8f29c-3a03-4220-9e47-d7b69fcd0c60\"],\"User-Agent\":[\"PostmanRuntime/7.26.8\"],\"Via\":[\"1.1 b7f480ddbe20bc339525f8e43ddce81a.cloudfront.net (CloudFront)\"],\"X-Amz-Cf-Id\":[\"4tuuGK8HjhXcH_delvMFFrsCvmBIjlK_WIlRx7_eusmtuAZ14QpNwA==\"],\"X-Amzn-Trace-Id\":[\"Root=1-5fd44c49-4a3cf23a43599a3b170070d6\"],\"X-Forwarded-For\":[\"64.58.33.5, 64.252.182.138\"],\"X-Forwarded-Port\":[\"443\"],\"X-Forwarded-Proto\":[\"https\"],\"X-Scope-Token\":[\"Bearer 222\"]},\"queryStringParameters\":{},\"multiValueQueryStringParameters\":{},\"pathParameters\":{},\"stageVariables\":{},\"requestContext\":{\"accountId\":\"964637446810\",\"stage\":\"Prod\",\"resourceId\":\"9b3qv0\",\"requestId\":\"2ca51c69-3090-407b-bf87-9b86185fadb0\",\"operationName\":null,\"identity\":{\"cognitoIdentityPoolId\":null,\"accountId\":null,\"cognitoIdentityId\":null,\"caller\":null,\"apiKey\":null,\"sourceIp\":\"64.58.33.5\",\"cognitoAuthenticationType\":null,\"cognitoAuthenticationProvider\":null,\"userArn\":null,\"userAgent\":\"PostmanRuntime/7.26.8\",\"user\":null,\"accessKey\":null},\"resourcePath\":\"/hello\",\"httpMethod\":\"GET\",\"apiId\":\"wievfduueb\",\"path\":\"/Prod/hello\",\"authorizer\":null},\"body\":null,\"isBase64Encoded\":null}";
    try {
      APIGatewayProxyRequestEvent request = objectMapper.readValue(json, APIGatewayProxyRequestEvent.class);
      AuthPolicy result = authorizer.handleRequest(request, new TestContext());
      System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
