package com.networknt.aws.lambda.middleware.validator;

import com.mservicetech.openapi.common.RequestEntity;
import com.mservicetech.openapi.validation.OpenApiValidator;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.HeaderValue;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ValidatorMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(ValidatorMiddleware.class);
    private static final String CONFIG_NAME = "lambda-validator";
    private static final String OPENAPI_NAME = "openapi.yaml";
    private static final String MISSING_EVENT_BODY = "ERR14007";
    private static final String EVENT_BODY_FAILED_VALIDATION = "ERR14008";
    private static final String VALIDATOR_FAILED_TO_LOAD_OPENAPI = "ERR14009";

    private static final ValidatorConfig CONFIG = (ValidatorConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

    OpenApiValidator openApiValidator;

    public ValidatorMiddleware(ChainLinkCallback middlewareCallback, LightLambdaExchange eventWrapper) {
        super(false, true, false, middlewareCallback, eventWrapper);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        LOG.debug("ValidatorHandler.executeMiddleware starts.");

        if (this.getChainDirection().equals(ChainDirection.REQUEST)
                && this.isApplicationJsonContentType(exchange.getRequest().getHeaders())
                && CONFIG.isValidateRequest()) {

            InputStream in  = this.getClass().getClassLoader().getResourceAsStream(OPENAPI_NAME);

            if (in != null) {
                openApiValidator = new OpenApiValidator(in);

            } else return new Status(VALIDATOR_FAILED_TO_LOAD_OPENAPI);

            try {
                in.close();
            } catch (IOException e) {
                LOG.error("Failed to close stream on openapi.yaml.");
                throw new RuntimeException(e);
            }

            if (exchange.getRequest().getBody() != null) {
                var requestEntity = new RequestEntity();
                requestEntity.setRequestBody(exchange.getRequest().getBody());
                requestEntity.setHeaderParameters(exchange.getRequest().getHeaders());
                requestEntity.setContentType(HeaderValue.APPLICATION_JSON);
                var status = openApiValidator.validateRequestPath(exchange.getRequest().getPath(), exchange.getRequest().getHttpMethod(), requestEntity);

                if (status != null)
                    return new Status(EVENT_BODY_FAILED_VALIDATION);

                else return LambdaMiddleware.successMiddlewareStatus();

            } else return new Status(MISSING_EVENT_BODY);

        } else if (this.getChainDirection().equals(ChainDirection.RESPONSE)
                && this.isApplicationJsonContentType(exchange.getResponse().getHeaders())
                && CONFIG.isValidateResponse()) {

            openApiValidator = new OpenApiValidator("config/" + OPENAPI_NAME);

            if (exchange.getResponse().getBody() != null) {

                // TODO - response body validation
                return LambdaMiddleware.successMiddlewareStatus();

            } else return new Status(MISSING_EVENT_BODY);
        }

        LOG.debug("ValidatorHandler.executeMiddleware ends.");

        return LambdaMiddleware.successMiddlewareStatus();
    }

    private boolean isApplicationJsonContentType(Map<String, String> headers) {
        return headers.containsKey(HeaderKey.CONTENT_TYPE)
                && headers.get(HeaderKey.CONTENT_TYPE).equals(HeaderValue.APPLICATION_JSON);
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        // TODO - update configs from external service
    }
}
