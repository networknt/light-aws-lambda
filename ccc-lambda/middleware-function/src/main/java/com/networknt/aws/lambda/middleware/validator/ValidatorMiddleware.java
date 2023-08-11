package com.networknt.aws.lambda.middleware.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.config.Config;
import com.networknt.openapi.OpenApiOperation;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidatorMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorMiddleware.class);
    private static final String CONFIG_NAME = "lambda-validator";

    static final String STATUS_MISSING_OPENAPI_OPERATION = "ERR10012";
    private static ValidatorConfig CONFIG = (ValidatorConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

    public ValidatorMiddleware(ChainLinkCallback middlewareCallback, LightLambdaExchange eventWrapper) {
        super(false, true, false, middlewareCallback, eventWrapper);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        LOG.debug("ValidatorHandler.handleRequest starts.");

        var reqPath = exchange.getRequest().getPath();
        // if request path is in the skipPathPrefixes in the config, call the next handler directly to skip the validation.
        if (CONFIG.getSkipPathPrefixes() != null && CONFIG.getSkipPathPrefixes().stream().anyMatch(reqPath::startsWith)) {
            LOG.debug("ValidatorHandler.handleRequest ends with skipped path '{}'", reqPath);
            return LambdaMiddleware.successMiddlewareStatus();
        }

        //final NormalisedPath requestPath = new ApiNormalisedPath(exchange.getRequestURI(), OpenApiHandler.getBasePath(exchange.getRequest().getPath()));

        OpenApiOperation openApiOperation = null;

        //Map<String, Object> auditInfo = exchange.get(AttachmentConstants.AUDIT_INFO);

        //if (auditInfo != null) {
        //    openApiOperation = (OpenApiOperation) auditInfo.get(Constants.OPENAPI_OPERATION_STRING);
        //}

        //if (openApiOperation == null) {
        //    LOG.debug("ValidatorHandler.handleRequest ends with an error.");
        //    return new LambdaStatus(LambdaStatus.Status.EXECUTION_FAILED, STATUS_MISSING_OPENAPI_OPERATION);
        //}
        //RequestValidator reqV = getRequestValidator(exchange.getRequestPath());
        //Status status = reqV.validateRequest(requestPath, exchange, openApiOperation);

        //if (status != null) {
        //    LOG.debug("ValidatorHandler.handleRequest ends with an error.");
        //    return new LambdaStatus(LambdaStatus.Status.EXECUTION_FAILED, );
        //}
        boolean valid = true;
        if (CONFIG.isValidateResponse())
            valid = validateResponse(exchange, openApiOperation);

        if (CONFIG.isValidateRequest())
            valid = validateRequest(exchange, openApiOperation);

        if (!valid)
            return new Status("");

        if (LOG.isDebugEnabled()) LOG.debug("ValidatorHandler.handleRequest ends.");

        return LambdaMiddleware.successMiddlewareStatus();
    }

    private boolean validateResponse(final LightLambdaExchange exchange, OpenApiOperation openApiOperation) {
        return true;
    }

    private boolean validateRequest(final LightLambdaExchange exchange, OpenApiOperation openApiOperation) {
        return true;
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        String configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, ValidatorConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
