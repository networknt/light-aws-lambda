package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static com.networknt.aws.lambda.AuthPolicy.PolicyDocument.getAllowOnePolicy;
import static com.networknt.aws.lambda.AuthPolicy.PolicyDocument.getDenyOnePolicy;

public class Authorizer implements RequestHandler<APIGatewayProxyRequestEvent, AuthPolicy> {
    ObjectMapper objectMapper = new ObjectMapper();

    public AuthPolicy handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        try {
            logger.log("EVENT: " + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        Map<String, String> headers = request.getHeaders();
        String authorization = headers.get("Authorization");
        String scopeToken = headers.get("X-Scope-Token");

        String primaryToken = authorization.substring(7);
        String secondaryToken = scopeToken.substring(7);

        Map<String, String> ctx = new HashMap<>();
        ctx.put("cid", "client_id");
        ctx.put("uid", "user_id");
        String principalId = "user_id"; // principalId should be user_id (AC) or client_id (CC) in the JWT token

        APIGatewayProxyRequestEvent.ProxyRequestContext proxyContext = request.getRequestContext();

        String region = System.getenv("AWS_REGION");  // use the env to get the region for REQUEST authorizer.
        String accountId = proxyContext.getAccountId();
        String apiId = proxyContext.getApiId();
        String stage = proxyContext.getStage();
        String httpMethod = proxyContext.getHttpMethod();
        if(primaryToken.equals("123") && secondaryToken.equals("456")) {
            logger.log("Allow " + String.format("arn:aws:execute-api:%s:%s:%s/%s/%s/%s", region, accountId, apiId, stage, httpMethod, "*"));
            return new AuthPolicy(principalId, getAllowOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx);
        } else {
            logger.log("Deny " + String.format("arn:aws:execute-api:%s:%s:%s/%s/%s/%s", region, accountId, apiId, stage, httpMethod, "*"));
            return new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx);
        }
    }
}
