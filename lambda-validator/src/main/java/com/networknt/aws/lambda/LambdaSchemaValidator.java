package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.validator.RequestValidator;
import com.networknt.aws.lambda.validator.SchemaValidator;
import com.networknt.config.Config;
import com.networknt.oas.model.Operation;
import com.networknt.oas.model.Path;
import com.networknt.openapi.*;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * @author Gavin Chen
 */
public class LambdaSchemaValidator {
    static final Logger logger = LoggerFactory.getLogger(LambdaSchemaValidator.class);
    private static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";

    static final String CONTENT_TYPE = "application/json";
    private static final String CONFIG_NAME = "openapi";
    private static final String SPEC_INJECT = "openapi-inject";
    public static ValidatorConfig config;
    public static OpenApiHelper helper;
    RequestValidator requestValidator;


    public LambdaSchemaValidator() {
        if (logger.isInfoEnabled()) logger.info("LambdaSchemaValidator is constructed");
        config = ValidatorConfig.load();
        Map<String, Object> inject = Config.getInstance().getJsonMapConfig(SPEC_INJECT);
        Map<String, Object> openapi = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
        openapi = OpenApiHelper.merge(openapi, inject);
        try {
            String openapiString = Config.getInstance().getMapper().writeValueAsString(openapi);
            if(logger.isTraceEnabled()) logger.trace("OpenApiMiddleware openapiString: " + openapiString);
            helper = new OpenApiHelper(openapiString);
        } catch (JsonProcessingException e) {
            logger.error("merge specification failed");
            throw new RuntimeException("merge specification failed");
        }
        final SchemaValidator schemaValidator = new SchemaValidator(helper.openApi3);
        this.requestValidator = new RequestValidator(schemaValidator, config);
    }

    /**
     * Validate the request based on the openapi.yaml specification
     *
     * @param requestEvent request event
     * @return responseEvent if error and null if pass.
     */
    public APIGatewayProxyResponseEvent validateRequest(APIGatewayProxyRequestEvent requestEvent) {
        if(logger.isTraceEnabled()) logger.trace("validateRequest starts");
        String reqPath = requestEvent.getPath();
        // if request path is in the skipPathPrefixes in the config, call the next handler directly to skip the validation.
        if(config.getSkipPathPrefixes() != null && config.getSkipPathPrefixes().stream().anyMatch(s -> reqPath.startsWith(s))) {
            if (logger.isDebugEnabled()) {
                logger.debug("validateRequest ends with skipped path {}", reqPath);
            }
            return null;
        }
        final NormalisedPath requestPath = new ApiNormalisedPath(reqPath, getBasePath(reqPath));
        if (logger.isTraceEnabled()) {
            logger.trace("requestPath original {} and normalized {}", requestPath.original(), requestPath.normalised());
        }
        final Optional<NormalisedPath> maybeApiPath = helper.findMatchingApiPath(requestPath);

        final NormalisedPath openApiPathString = maybeApiPath.get();
        final Path path = helper.openApi3.getPath(openApiPathString.original());

        final String httpMethod = requestEvent.getHttpMethod().toLowerCase();
        final Operation operation = path.getOperation(httpMethod);

        if (operation == null) {
            Status status = new Status(STATUS_METHOD_NOT_ALLOWED, httpMethod, openApiPathString.normalised());
            return createErrorResponse(status.getStatusCode(), status.getCode(), status.getDescription());
        }

        // This handler can identify the openApiOperation and endpoint only. Other info will be added by JwtVerifyHandler.
        final OpenApiOperation openApiOperation = new OpenApiOperation(openApiPathString, path, httpMethod, operation);
        Status status = requestValidator.validateRequest(requestPath, requestEvent, openApiOperation);
        if (status !=null) {
            return createErrorResponse(status.getStatusCode(), status.getCode(), status.getDescription());
        }
        return null;
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorCode, String errorMessage) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String body = "{\"statusCode\":" + statusCode + ",\"code\":\"" + errorCode + ",\"description\":\"" + errorMessage + "\"}";
        if (logger.isDebugEnabled()) {
            logger.debug("error info:" + body);
        }
        return new APIGatewayProxyResponseEvent()
                .withHeaders(headers)
                .withStatusCode(statusCode)
                .withBody(body);
    }

    // this is used to get the basePath from the OpenApiMiddleware.
    public static String getBasePath(String requestPath) {
        String basePath = "";
        // assume there is a single spec.
        if (helper != null) {
            basePath = helper.basePath;
            if (logger.isTraceEnabled())
                logger.trace("Found basePath for single spec from OpenApiMiddleware helper: {}", basePath);
        }
        return basePath;
    }
}
