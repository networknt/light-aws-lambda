package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.oas.model.Operation;
import com.networknt.oas.model.Path;
import com.networknt.oas.model.SecurityParameter;
import com.networknt.oas.model.SecurityRequirement;
import com.networknt.openapi.ApiNormalisedPath;
import com.networknt.openapi.NormalisedPath;
import com.networknt.openapi.OpenApiHelper;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * It is called in the Lambda framework to verify the scopes in the primary and secondary jwt tokens.
 * The scopes are passed by the jwt-authorizer deployed on the API Gateway after verify JWT tokens. The
 * authorizer can not verify the scopes as it doesn't have the knowledge of the OpenAPI specification.
 *
 * Each function will have the openapi.yaml packaged as configuration and this class will use it to
 * verify the scopes again the scopes in the authorizer context enriched by the authorizer.
 *
 * The verifyScope is called by the request-handler that intercepts the request and response in the App.
 *
 * The light-rest-4j now supports multiple OpenApi specifications, however, the Lambda should only use
 * one specification. The default config should do that job as it is configured as single spec.
 *
 * @author Steve Hu
 */
public class ScopeVerifier {
    static final Logger logger = LoggerFactory.getLogger(ScopeVerifier.class);
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";
    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    static final String STATUS_MISSING_GATEWAY_AUTHORIZER = "ERR10061";
    static final String STATUS_MISSING_PRIMARY_SCOPES = "ERR10062";

    /**
     * verify the scopes from the primary and optional secondary tokens against the scopes in the
     * openapi.yaml specification.
     *
     * @param requestEvent request event
     * @return responseEvent if error and null if pass.
     */
    public APIGatewayProxyResponseEvent verifyScope(APIGatewayProxyRequestEvent requestEvent) {
        APIGatewayProxyRequestEvent.ProxyRequestContext context = requestEvent.getRequestContext();
        if(context != null) {
            // get the enriched primaryScopes and convert it into a list of string again.
            Map<String, Object> authorizerMap = context.getAuthorizer();
            if(authorizerMap == null) {
                logger.error("Authorizer enriched context is missing");
                return createErrorResponse(401, STATUS_MISSING_GATEWAY_AUTHORIZER);
            }
            String primaryScopesString = (String) authorizerMap.get(Constants.PRIMARY_SCOPES);
            if (primaryScopesString != null) {
                String[] primaryScopes = StringUtils.split(primaryScopesString, ' ');
                // get the openapi.yaml from the resources folder.
                String spec = new Scanner(ScopeVerifier.class.getClassLoader().getResourceAsStream("openapi.yaml"), StandardCharsets.UTF_8).useDelimiter("\\A").next();
                OpenApiHelper openApiHelper = null;
                if (spec != null) {
                    openApiHelper = new OpenApiHelper(spec);
                }
                if (openApiHelper.openApi3 != null) {
                    String path = requestEvent.getPath();
                    final NormalisedPath requestPath = new ApiNormalisedPath(path, openApiHelper.basePath);
                    final Optional<NormalisedPath> maybeApiPath = openApiHelper.findMatchingApiPath(requestPath);
                    if (!maybeApiPath.isPresent()) {
                        logger.error("Invalid request path " + path);
                        return createErrorResponse(404, STATUS_INVALID_REQUEST_PATH);
                    }

                    final NormalisedPath swaggerPathString = maybeApiPath.get();
                    final Path swaggerPath = openApiHelper.openApi3.getPath(swaggerPathString.original());

                    final String httpMethod = requestEvent.getHttpMethod().toLowerCase();
                    Operation operation = swaggerPath.getOperation(httpMethod);
                    if (operation == null) {
                        logger.error("Method " + httpMethod + " is not allowed");
                        return createErrorResponse(405, STATUS_METHOD_NOT_ALLOWED);
                    }
                    // save the endpoint for metric collection
                    context.getAuthorizer().put(Constants.ENDPOINT_STRING, swaggerPathString.normalised() + "@" + httpMethod);

                    // get scope defined in OpenAPI spec for this endpoint.
                    Collection<String> specScopes = null;
                    Collection<SecurityRequirement> securityRequirements = operation.getSecurityRequirements();
                    if (securityRequirements != null) {
                        for (SecurityRequirement requirement : securityRequirements) {
                            SecurityParameter securityParameter = null;
                            for (String oauth2Name : openApiHelper.oauth2Names) {
                                securityParameter = requirement.getRequirement(oauth2Name);
                                if (securityParameter != null) break;
                            }
                            if (securityParameter != null) specScopes = securityParameter.getParameters();
                            if (specScopes != null) break;
                        }
                    }

                    // is there a scope token
                    String secondaryScopesString = (String) context.getAuthorizer().get(Constants.SECONDARY_SCOPES);
                    if (secondaryScopesString != null) {
                        String[] secondaryScopes = StringUtils.split(secondaryScopesString, ' ');
                        if (!matchedScopes(secondaryScopes, specScopes)) {
                            logger.error("Scopes " + secondaryScopesString + " in scope token and spec scopes " + specScopes + " are not matched");
                            return createErrorResponse(403, STATUS_SCOPE_TOKEN_SCOPE_MISMATCH);
                        }
                    } else {
                        // no scope token, verify scope from auth token.
                        if (!matchedScopes(primaryScopes, specScopes)) {
                            logger.error("Scopes " + primaryScopesString + " in authorization token and spec scopes " + specScopes + " are not matched");
                            return createErrorResponse(403, STATUS_AUTH_TOKEN_SCOPE_MISMATCH);
                        }
                    }
                }
            } else {
                // not primary scopes available.
                logger.error("Scopes from the JWT token in Authorization header are missing");
                return createErrorResponse(401, STATUS_MISSING_PRIMARY_SCOPES);
            }
        }
        return null;
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorCode) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String body = "{\"statusCode\":" + statusCode + ",\"code\":\"" + errorCode + "\"}";
        return new APIGatewayProxyResponseEvent()
                .withHeaders(headers)
                .withStatusCode(statusCode)
                .withBody(body);
    }

    private boolean matchedScopes(String[] scopes, Collection<String> specScopes) {
        boolean matched = false;
        if(specScopes != null && specScopes.size() > 0) {
            if(scopes != null && scopes.length > 0) {
                List<String> jwtScopes = Arrays.asList(scopes);
                for(String scope: specScopes) {
                    if(jwtScopes.contains(scope)) {
                        matched = true;
                        break;
                    }
                }
            }
        } else {
            matched = true;
        }
        return matched;
    }
}
