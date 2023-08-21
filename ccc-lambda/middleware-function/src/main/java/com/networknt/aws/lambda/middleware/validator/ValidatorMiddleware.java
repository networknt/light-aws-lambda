package com.networknt.aws.lambda.middleware.validator;

import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.config.Config;
import com.networknt.openapi.*;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.networknt.openapi.RequestValidator;

import java.util.Map;
import java.util.Optional;

public class ValidatorMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorMiddleware.class);
    private static final String CONFIG_NAME = "lambda-validator";

    private static ValidatorConfig CONFIG = (ValidatorConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

    RequestValidator requestValidator;
    Map<String, RequestValidator> requestValidatorMap;

    ResponseValidator responseValidator;
    Map<String, ResponseValidator> responseValidatorMap;

    public ValidatorMiddleware(ChainLinkCallback middlewareCallback, LightLambdaExchange eventWrapper) {
        super(false, true, false, middlewareCallback, eventWrapper);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        LOG.debug("ValidatorHandler.handleRequest starts.");



        LOG.debug("ValidatorHandler.handleRequest ends.");

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
    }
}
