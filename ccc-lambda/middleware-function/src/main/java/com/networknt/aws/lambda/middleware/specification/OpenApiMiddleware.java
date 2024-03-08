package com.networknt.aws.lambda.middleware.specification;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.config.Config;
import com.networknt.oas.model.Operation;
import com.networknt.oas.model.Path;
import com.networknt.openapi.ApiNormalisedPath;
import com.networknt.openapi.NormalisedPath;
import com.networknt.openapi.OpenApiHelper;
import com.networknt.openapi.OpenApiOperation;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.networknt.aws.lambda.middleware.audit.AuditMiddleware.AUDIT_ATTACHMENT_KEY;

public class OpenApiMiddleware extends LambdaMiddleware implements MiddlewareHandler {
    @Override
    public void register() {

    }

    @Override
    public void reload() {

    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        return null;
    }

    private static final Logger LOG = LoggerFactory.getLogger(OpenApiMiddleware.class);
    private static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    private static final String CONFIG_NAME = "lambda-openapi";
    private static final String SPEC_INJECT = "openapi-inject";

    public static OpenApiHelper helper;

    public OpenApiMiddleware() {
        super(false, false, false);
        Map<String, Object> inject = Config.getInstance().getJsonMapConfig(SPEC_INJECT);
        Map<String, Object> openapi = Config.getInstance().getJsonMapConfigNoCache(CONFIG_NAME);
        OpenApiHelper.merge(openapi, inject);
        try {
            helper = new OpenApiHelper(Config.getInstance().getMapper().writeValueAsString(openapi));
        } catch (JsonProcessingException e) {
            LOG.error("merge specification failed");
            throw new RuntimeException("merge specification failed");
        }
    }

    @Override
    protected Status executeMiddleware(LightLambdaExchange exchange) throws InterruptedException {

        LOG.debug("OpenAPI Specification Time - Start: {}", System.currentTimeMillis());

        if (LOG.isDebugEnabled())
            LOG.debug("OpenApiMiddleware.executeMiddleware starts.");

        final NormalisedPath requestPath = new ApiNormalisedPath(exchange.getRequest().getPath(), helper.basePath);
        final Optional<NormalisedPath> maybeApiPath = helper.findMatchingApiPath(requestPath);

        final NormalisedPath openApiPathString = maybeApiPath.get();
        final Path path = helper.openApi3.getPath(openApiPathString.original());

        final String httpMethod = exchange.getRequest().getHttpMethod().toLowerCase();
        final Operation operation = path.getOperation(httpMethod);

        if (operation == null) {
            return new Status(STATUS_METHOD_NOT_ALLOWED, httpMethod, openApiPathString.normalised());
        }

        // This handler can identify the openApiOperation and endpoint only. Other info will be added by JwtVerifyHandler.
        final OpenApiOperation openApiOperation = new OpenApiOperation(openApiPathString, path, httpMethod, operation);

        String endpoint = openApiPathString.normalised() + "@" + httpMethod.toLowerCase();
        Map<String, Object> auditInfo = (exchange.getRequestAttachment(AUDIT_ATTACHMENT_KEY) != null) ? (Map<String, Object>) exchange.getRequestAttachment(AUDIT_ATTACHMENT_KEY) : new HashMap<>();
        auditInfo.put(Constants.ENDPOINT_STRING, endpoint);
        auditInfo.put(Constants.OPENAPI_OPERATION_STRING, openApiOperation);
        exchange.addRequestAttachment(AUDIT_ATTACHMENT_KEY, auditInfo);

        if (LOG.isDebugEnabled())
            LOG.debug("OpenApiMiddleware.executeMiddleware ends.");

        LOG.debug("OpenAPI Specification Time - Finish: {}", System.currentTimeMillis());

        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {
        // TODO
    }

    @Override
    public boolean isEnabled() {

        // TODO - do we have a config for this middleware?
        return true;
    }

}
