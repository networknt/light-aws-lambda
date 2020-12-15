package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.utility.Constants;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.networknt.aws.lambda.AuthPolicy.PolicyDocument.getAllowOnePolicy;
import static com.networknt.aws.lambda.AuthPolicy.PolicyDocument.getDenyOnePolicy;

public class Authorizer implements RequestHandler<APIGatewayProxyRequestEvent, AuthPolicy> {
    private static Logger logger = LoggerFactory.getLogger(Authorizer.class);

    public static JwtVerifier jwtVerifier;
    static {
        jwtVerifier = new JwtVerifier();
    }

    ObjectMapper objectMapper = new ObjectMapper();

    public AuthPolicy handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            logger.debug(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        APIGatewayProxyRequestEvent.ProxyRequestContext proxyContext = request.getRequestContext();
        String region = System.getenv("AWS_REGION");  // use the env to get the region for REQUEST authorizer.
        String accountId = proxyContext.getAccountId();
        String apiId = proxyContext.getApiId();
        String stage = proxyContext.getStage();
        String httpMethod = proxyContext.getHttpMethod();
        String arn = String.format("arn:aws:execute-api:%s:%s:%s/%s/%s/%s", region, accountId, apiId, stage, httpMethod, "*");
        String principalId = null;
        Map<String, String> headers = request.getHeaders();
        String authorization = headers.get("Authorization");
        String scopeToken = headers.get("X-Scope-Token");
        String primaryToken = authorization.substring(7);
        String secondaryToken = scopeToken == null ? null : scopeToken.substring(7);
        Map<String, String> ctx = new HashMap<>();
        try {
            JwtClaims claims = jwtVerifier.verifyJwt(primaryToken, false);
            // handle the primary token.
            String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);
            // try to get the cid as some OAuth tokens name it as cid like Okta.
            if(clientId == null) clientId = claims.getStringClaimValue(Constants.CID_STRING);
            ctx.put(Constants.CLIENT_ID_STRING, clientId);
            String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
            // try to get the uid as some OAuth tokens name it as uid like Okta.
            if(userId == null) userId = claims.getStringClaimValue(Constants.UID_STRING);
            if(userId != null) {
                ctx.put(Constants.USER_ID_STRING, userId);
                principalId = userId;
            } else {
                principalId = clientId;
            }
            // primary scopes
            List<String> primaryScopes = null;
            Object scopeClaim = claims.getClaimValue(Constants.SCOPE_STRING);
            if(scopeClaim instanceof String) {
                primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
            } else if(scopeClaim instanceof List) {
                primaryScopes = claims.getStringListClaimValue(Constants.SCOPE_STRING);
            }
            if(primaryScopes == null || primaryScopes.isEmpty()) {
                // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                Object scpClaim = claims.getClaimValue(Constants.SCP_STRING);
                if(scpClaim instanceof String) {
                    primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                } else if(scpClaim instanceof List) {
                    primaryScopes = claims.getStringListClaimValue(Constants.SCP_STRING);
                }
            }
            ctx.put(Constants.PRIMARY_SCOPES, primaryScopes.toString());
        } catch (InvalidJwtException e) {
            logger.error("ERR10000 InvalidJwtException:", e);
            return new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx);
        } catch (ExpiredTokenException e) {
            logger.error("ERR10001 ExpiredTokenException", e);
            return new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx);
        } catch (MalformedClaimException e) {
            logger.error("ERR10000 MalformedClaimException", e);
            return new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx);
        }
        logger.debug("Allow " + arn);
        return new AuthPolicy(principalId, getAllowOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx);
    }
}
