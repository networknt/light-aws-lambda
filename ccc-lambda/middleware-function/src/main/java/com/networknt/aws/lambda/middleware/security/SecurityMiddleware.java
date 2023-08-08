package com.networknt.aws.lambda.middleware.security;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.config.Config;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.networknt.aws.lambda.middleware.security.AuthPolicy.PolicyDocument.getAllowOnePolicy;
import static com.networknt.aws.lambda.middleware.security.AuthPolicy.PolicyDocument.getDenyOnePolicy;

@ChainProperties(asynchronous = true, audited = false)
public class SecurityMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityMiddleware.class);
    private static final String CONFIG_NAME = "security";

    private static SecurityConfig CONFIG = (SecurityConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, SecurityConfig.class);
    private static final LambdaEventWrapper.Attachable<SecurityMiddleware> SECURITY_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(SecurityMiddleware.class);

    public SecurityMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {
        try {
            LOG.debug(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this.eventWrapper.getRequest()));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        APIGatewayProxyRequestEvent.ProxyRequestContext proxyContext = this.eventWrapper.getRequest().getRequestContext();

        String region = System.getenv("AWS_REGION");  // use the env to get the region for REQUEST authorizer.
        String accountId = proxyContext.getAccountId();
        String apiId = proxyContext.getApiId();
        String stage = proxyContext.getStage();
        String httpMethod = proxyContext.getHttpMethod();
        String arn = String.format("arn:aws:execute-api:%s:%s:%s/%s/%s/%s", region, accountId, apiId, stage, httpMethod, "*");
        String principalId = null;
        Map<String, String> headers = this.eventWrapper.getRequest().getHeaders();
        String authorization = headers.get("Authorization");

        LOG.debug("authorization = " + authorization);

        String scopeToken = headers.get("X-Scope-Token");
        String primaryToken = authorization.substring(7);

        LOG.debug("primaryToken = " + primaryToken);

        String secondaryToken = scopeToken == null ? null : scopeToken.substring(7);
        Map<String, String> ctx = new HashMap<>();

        try {

            boolean ignoreExpiry = CONFIG.getIgnoreJwtExpiry();

            LOG.debug("ignoreExpiry = " + ignoreExpiry);

            JwtVerifier jwtVerifier = new JwtVerifier(stage);
            JwtClaims claims = jwtVerifier.verifyJwt(primaryToken, ignoreExpiry);

            // handle the primary token.
            String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);

            // try to get the cid as some OAuth tokens name it as cid like Okta.
            if (clientId == null)
                clientId = claims.getStringClaimValue(Constants.CID_STRING);

            ctx.put(Constants.CLIENT_ID_STRING, clientId);
            String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);

            // try to get the uid as some OAuth tokens name it as uid like Okta.
            if (userId == null)
                userId = claims.getStringClaimValue(Constants.UID_STRING);

            if (userId != null) {
                ctx.put(Constants.USER_ID_STRING, userId);
                principalId = userId;
            } else {
                principalId = clientId;
            }

            // primary scopes
            List<String> primaryScopes = null;
            Object scopeClaim = claims.getClaimValue(Constants.SCOPE_STRING);

            if (scopeClaim instanceof String) {
                primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
            } else if (scopeClaim instanceof List) {
                primaryScopes = claims.getStringListClaimValue(Constants.SCOPE_STRING);
            }

            if (primaryScopes == null || primaryScopes.isEmpty()) {

                // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                Object scpClaim = claims.getClaimValue(Constants.SCP_STRING);

                if (scpClaim instanceof String) {
                    primaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                } else if (scpClaim instanceof List) {
                    primaryScopes = claims.getStringListClaimValue(Constants.SCP_STRING);
                }
            }

            ctx.put(Constants.PRIMARY_SCOPES, StringUtils.join(primaryScopes, ' '));

            // secondary scopes
            if (secondaryToken != null) {
                claims = jwtVerifier.verifyJwt(secondaryToken, ignoreExpiry);
                List<String> secondaryScopes = null;
                scopeClaim = claims.getClaimValue(Constants.SCOPE_STRING);

                if (scopeClaim instanceof String) {
                    secondaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
                } else if (scopeClaim instanceof List) {
                    secondaryScopes = claims.getStringListClaimValue(Constants.SCOPE_STRING);
                }

                if (secondaryScopes == null || secondaryScopes.isEmpty()) {
                    // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                    Object scpClaim = claims.getClaimValue(Constants.SCP_STRING);
                    if (scpClaim instanceof String) {
                        secondaryScopes = Arrays.asList(claims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                    } else if (scpClaim instanceof List) {
                        secondaryScopes = claims.getStringListClaimValue(Constants.SCP_STRING);
                    }
                }
                ctx.put(Constants.SCOPE_CLIENT_ID_STRING, claims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                ctx.put(Constants.SECONDARY_SCOPES, StringUtils.join(secondaryScopes, ' '));
            }

        } catch (InvalidJwtException e) {
            LOG.error("ERR10000 InvalidJwtException:", e);
            this.eventWrapper.addRequestAttachment(SECURITY_ATTACHMENT_KEY, new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx));

            return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, "ERR10000");

        } catch (ExpiredTokenException e) {
            LOG.error("ERR10001 ExpiredTokenException", e);
            this.eventWrapper.addRequestAttachment(SECURITY_ATTACHMENT_KEY, new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx));

            return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, "ERR10001");

        } catch (MalformedClaimException e) {
            LOG.error("ERR10000 MalformedClaimException", e);
            this.eventWrapper.addRequestAttachment(SECURITY_ATTACHMENT_KEY, new AuthPolicy(principalId, getDenyOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx));

            return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, "ERR10000");
        }

        LOG.debug("Allow " + arn);
        this.eventWrapper.addRequestAttachment(SECURITY_ATTACHMENT_KEY, new AuthPolicy(principalId, getAllowOnePolicy(region, accountId, apiId, stage, AuthPolicy.HttpMethod.valueOf(httpMethod), "*"), ctx));

        return ChainLinkReturn.successMiddlewareReturn();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {

        LOG.debug("Grabbing config for SecurityMiddleware instance...");

        String configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, SecurityConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
