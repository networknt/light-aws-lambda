package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.oas.model.Operation;
import com.networknt.oas.model.Path;
import com.networknt.oas.model.SecurityParameter;
import com.networknt.oas.model.SecurityRequirement;
import com.networknt.utility.Constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * It is called in the framework to verify the scopes in the primary and secondary jwt tokens.
 *
 * @author Steve Hu
 */
public class ScopeVerifier {
    /**
     *
     * @param requestEvent request event
     * @return responseEvent if error and null if pass.
     */
    public APIGatewayProxyResponseEvent verifyScope(APIGatewayProxyRequestEvent requestEvent) {
        APIGatewayProxyResponseEvent responseEvent = null;
        APIGatewayProxyRequestEvent.ProxyRequestContext context = requestEvent.getRequestContext();
        if(context != null) {
            // get the enriched primaryScopes and convert it into a list of string again.
            String primaryScopes = (String)context.getAuthorizer().get(Constants.PRIMARY_SCOPES);
            List<String> scopes = null;
            if(primaryScopes != null) {

            }
            /*
            try {
                JwtClaims claims = jwtVerifier.verifyJwt(jwt, false);
                String clientId = claims.getStringClaimValue(Constants.CLIENT_ID_STRING);
                // try to get the cid as some OAuth tokens name it as cid like Okta.
                if(clientId == null) clientId = claims.getStringClaimValue(Constants.CID_STRING);
                ctx.put(Constants.CLIENT_ID_STRING, clientId);
                String userId = claims.getStringClaimValue(Constants.USER_ID_STRING);
                // try to get the uid as some OAuth tokens name it as uid like Okta.
                if(userId == null) userId = claims.getStringClaimValue(Constants.UID_STRING);
                ctx.put(Constants.USER_ID_STRING, userId);
                if(OpenApiHelper.openApi3 != null) {
                    Operation operation = null;
                    OpenApiOperation openApiOperation = (OpenApiOperation)auditInfo.get(Constants.OPENAPI_OPERATION_STRING);
                    if(openApiOperation == null) {
                        final NormalisedPath requestPath = new ApiNormalisedPath(exchange.getRequestURI());
                        final Optional<NormalisedPath> maybeApiPath = OpenApiHelper.findMatchingApiPath(requestPath);
                        if (!maybeApiPath.isPresent()) {
                            setExchangeStatus(exchange, STATUS_INVALID_REQUEST_PATH);
                            return;
                        }

                        final NormalisedPath swaggerPathString = maybeApiPath.get();
                        final Path swaggerPath = OpenApiHelper.openApi3.getPath(swaggerPathString.original());

                        final String httpMethod = exchange.getRequestMethod().toString().toLowerCase();
                        operation = swaggerPath.getOperation(httpMethod);

                        if (operation == null) {
                            setExchangeStatus(exchange, STATUS_METHOD_NOT_ALLOWED);
                            return;
                        }
                        openApiOperation = new OpenApiOperation(swaggerPathString, swaggerPath, httpMethod, operation);
                        auditInfo.put(Constants.OPENAPI_OPERATION_STRING, openApiOperation);
                        auditInfo.put(Constants.ENDPOINT_STRING, swaggerPathString.normalised() + "@" + httpMethod);
                    } else {
                        operation = openApiOperation.getOperation();
                    }

                    // is there a scope token
                    String scopeHeader = headerMap.getFirst(HttpStringConstants.SCOPE_TOKEN);
                    String scopeJwt = jwtVerifier.getJwtFromAuthorization(scopeHeader);
                    List<String> secondaryScopes = null;
                    if(scopeJwt != null) {
                        try {
                            JwtClaims scopeClaims = jwtVerifier.verifyJwt(scopeJwt, false);
                            Object scopeClaim = scopeClaims.getClaimValue(Constants.SCOPE_STRING);
                            if(scopeClaim instanceof String) {
                                secondaryScopes = Arrays.asList(scopeClaims.getStringClaimValue(Constants.SCOPE_STRING).split(" "));
                            } else if(scopeClaim instanceof List) {
                                secondaryScopes = scopeClaims.getStringListClaimValue(Constants.SCOPE_STRING);
                            }
                            if(secondaryScopes == null || secondaryScopes.isEmpty()) {
                                // some IDPs like Okta and Microsoft call scope claim "scp" instead of "scope"
                                Object scpClaim = scopeClaims.getClaimValue(Constants.SCP_STRING);
                                if(scpClaim instanceof String) {
                                    secondaryScopes = Arrays.asList(scopeClaims.getStringClaimValue(Constants.SCP_STRING).split(" "));
                                } else if(scpClaim instanceof List) {
                                    secondaryScopes = scopeClaims.getStringListClaimValue(Constants.SCP_STRING);
                                }
                            }
                            auditInfo.put(Constants.SCOPE_CLIENT_ID_STRING, scopeClaims.getStringClaimValue(Constants.CLIENT_ID_STRING));
                            auditInfo.put(Constants.ACCESS_CLAIMS, scopeClaims);
                        } catch (InvalidJwtException | MalformedClaimException e) {
                            logger.error("InvalidJwtException", e);
                            setExchangeStatus(exchange, STATUS_INVALID_SCOPE_TOKEN);
                            return;
                        } catch (ExpiredTokenException e) {
                            logger.error("ExpiredTokenException", e);
                            setExchangeStatus(exchange, STATUS_SCOPE_TOKEN_EXPIRED);
                            return;
                        }
                    }

                    // validate the scope against the scopes configured in the OpenAPI spec
                    if((Boolean)config.get(ENABLE_VERIFY_SCOPE)) {
                        // get scope defined in OpenAPI spec for this endpoint.
                        Collection<String> specScopes = null;
                        Collection<SecurityRequirement> securityRequirements = operation.getSecurityRequirements();
                        if(securityRequirements != null) {
                            for(SecurityRequirement requirement: securityRequirements) {
                                SecurityParameter securityParameter = null;
                                for(String oauth2Name: OpenApiHelper.oauth2Names) {
                                    securityParameter = requirement.getRequirement(oauth2Name);
                                    if(securityParameter != null) break;
                                }
                                if(securityParameter != null) specScopes = securityParameter.getParameters();
                                if(specScopes != null) break;
                            }
                        }

                        // validate scope
                        if (scopeHeader != null) {
                            if (secondaryScopes == null || !matchedScopes(secondaryScopes, specScopes)) {
                                setExchangeStatus(exchange, STATUS_SCOPE_TOKEN_SCOPE_MISMATCH, secondaryScopes, specScopes);
                                return;
                            }
                        } else {
                            // no scope token, verify scope from auth token.
                            List<String> primaryScopes = null;
                            try {
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
                            } catch (MalformedClaimException e) {
                                logger.error("MalformedClaimException", e);
                                setExchangeStatus(exchange, STATUS_INVALID_AUTH_TOKEN);
                                return;
                            }
                            if (!matchedScopes(primaryScopes, specScopes)) {
                                setExchangeStatus(exchange, STATUS_AUTH_TOKEN_SCOPE_MISMATCH, primaryScopes, specScopes);
                                return;
                            }
                        }
                    } // end scope validation
                }
            } catch (InvalidJwtException e) {
                // only log it and unauthorized is returned.
                logger.error("InvalidJwtException: ", e);
                setExchangeStatus(exchange, STATUS_INVALID_AUTH_TOKEN);
            } catch (ExpiredTokenException e) {
                logger.error("ExpiredTokenException", e);
                setExchangeStatus(exchange, STATUS_AUTH_TOKEN_EXPIRED);
            } catch (MalformedClaimException e) {
                logger.error("MalformedClaimException", e);

            }
            */
        } else {

        }
        return responseEvent;
    }
}
