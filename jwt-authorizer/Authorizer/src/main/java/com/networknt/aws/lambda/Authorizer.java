package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.config.JsonMapper;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.networknt.aws.lambda.AuthPolicy.PolicyDocument.getAllowOnePolicy;
import static com.networknt.aws.lambda.AuthPolicy.PolicyDocument.getDenyOnePolicy;

public class Authorizer implements RequestHandler<APIGatewayProxyRequestEvent, AuthPolicy> {
    private static final Logger logger = LoggerFactory.getLogger(Authorizer.class);

    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Map<String, Object>> config = Configuration.getInstance().getConfig();

    public AuthPolicy handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        logger.debug(JsonMapper.toJson(request));
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
        logger.debug("authorization = " + authorization);
        String scopeToken = headers.get("X-Scope-Token");
        String primaryToken = authorization.substring(7);
        logger.debug("primaryToken = " + primaryToken);
        String secondaryToken = scopeToken == null ? null : scopeToken.substring(7);
        Map<String, String> ctx = new HashMap<>();
        try {
            Map<String, Object> stageConfig = config.get(stage);
            Boolean ignoreExpiry = false;
            if(stageConfig != null) {
                ignoreExpiry = (Boolean)stageConfig.get("ignoreJwtExpiry");
            }
            logger.debug("ignoreExpiry = " + ignoreExpiry);
            JwtVerifier jwtVerifier = new JwtVerifier(stage);
            JwtClaims claims = jwtVerifier.verifyJwt(primaryToken, ignoreExpiry == null ? false : ignoreExpiry);
            // handle the primary token.
            String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);
            // try to get the cid as some OAuth tokens name it as cid like Okta.
            if(clientId == null) clientId = claims.getStringClaimValue(Constants.CID);
            ctx.put(Constants.CLIENT_ID_STRING, clientId);
            String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
            // try to get the uid as some OAuth tokens name it as uid like Okta.
            if(userId == null) userId = claims.getStringClaimValue(Constants.UID);
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
            ctx.put(Constants.PRIMARY_SCOPES, StringUtils.join(primaryScopes, ' '));
            // secondary scopes
            if(secondaryToken != null) {
                claims = jwtVerifier.verifyJwt(secondaryToken, ignoreExpiry == null ? false : ignoreExpiry);
                List<String> secondaryScopes = null;
                scopeClaim = claims.getClaimValue(Constants.SCOPE_STRING);
                if(scopeClaim instanceof String) {
                    secondaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
                } else if(scopeClaim instanceof List) {
                    secondaryScopes = claims.getStringListClaimValue(Constants.SCOPE_STRING);
                }
                if(secondaryScopes == null || secondaryScopes.isEmpty()) {
                    // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                    Object scpClaim = claims.getClaimValue(Constants.SCP_STRING);
                    if(scpClaim instanceof String) {
                        secondaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                    } else if(scpClaim instanceof List) {
                        secondaryScopes = claims.getStringListClaimValue(Constants.SCP_STRING);
                    }
                }
                ctx.put(Constants.SCOPE_CLIENT_ID_STRING, claims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                ctx.put(Constants.SECONDARY_SCOPES, StringUtils.join(secondaryScopes, ' '));
            }
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
