package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.jsonoverlay.Overlay;
import com.networknt.oas.model.*;
import com.networknt.oas.model.impl.SchemaImpl;
import com.networknt.openapi.ApiNormalisedPath;
import com.networknt.openapi.NormalisedPath;
import com.networknt.openapi.OpenApiHelper;
import com.networknt.openapi.OpenApiOperation;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * It is called in the Lambda framework to validate the request against the openapi.yaml specification.
 *
 * Each function will have the openapi.yaml packaged as configuration and this class will use it to
 * validate the request headers, query parameters, path parameters and body based on the json schema.
 *
 * The validateRequest is called by the request-handler that intercepts the request and response in the App.
 *
 * @author Steve Hu
 */
public class OpenApiValidator {
    static final Logger logger = LoggerFactory.getLogger(OpenApiValidator.class);
    static final String STATUS_AUTH_TOKEN_SCOPE_MISMATCH = "ERR10005";
    static final String STATUS_SCOPE_TOKEN_SCOPE_MISMATCH = "ERR10006";
    static final String STATUS_INVALID_REQUEST_PATH = "ERR10007";
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    public OpenApiValidator() {
    }
    /**
     * Validate the request based on the openapi.yaml specification
     *
     * @param requestEvent request event
     * @return responseEvent if error and null if pass.
     */
    public APIGatewayProxyResponseEvent validateRequest(APIGatewayProxyRequestEvent requestEvent) {
        String path = requestEvent.getPath();
        String spec = new Scanner(OpenApiValidator.class.getClassLoader().getResourceAsStream("openapi.yaml"), StandardCharsets.UTF_8).useDelimiter("\\A").next();
        OpenApiHelper openApiHelper = null;
        if (spec != null) {
            openApiHelper = OpenApiHelper.init(spec);
        }
        if (OpenApiHelper.openApi3 != null) {
            final NormalisedPath requestPath = new ApiNormalisedPath(path);
            final Optional<NormalisedPath> maybeApiPath = openApiHelper.findMatchingApiPath(requestPath);
            if (!maybeApiPath.isPresent()) {
                logger.error("Invalid request path " + path);
                return createErrorResponse(404, STATUS_INVALID_REQUEST_PATH);
            }

            final NormalisedPath swaggerPathString = maybeApiPath.get();
            final Path swaggerPath = OpenApiHelper.openApi3.getPath(swaggerPathString.original());

            final String httpMethod = requestEvent.getHttpMethod().toLowerCase();
            Operation operation = swaggerPath.getOperation(httpMethod);
            if (operation == null) {
                logger.error("Method " + httpMethod + " is not allowed");
                return createErrorResponse(405, STATUS_METHOD_NOT_ALLOWED);
            }
            OpenApiOperation openApiOperation = new OpenApiOperation(swaggerPathString, swaggerPath, httpMethod, operation);
            final SchemaValidator schemaValidator = new SchemaValidator(OpenApiHelper.openApi3);


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


}
