package com.networknt.aws.lambda.middleware.validator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.config.Config;

@ChainProperties(asynchronous = true, audited = false)
public class ValidatorMiddleware extends LambdaMiddleware {

    private static final String CONFIG_NAME = "validator";
    private static ValidatorConfig CONFIG = (ValidatorConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

    public ValidatorMiddleware(ChainLinkCallback middlewareCallback, LightLambdaExchange eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
        return ChainLinkReturn.successMiddlewareReturn();
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
