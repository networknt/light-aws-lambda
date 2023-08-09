package com.networknt.aws.lambda.middleware.limit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChainProperties(asynchronous = true, audited = false)
public class LimitMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);
    private static final String CONFIG_NAME = "limit";
    private static LimitConfig CONFIG = (LimitConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LimitConfig.class);

    public LimitMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return ChainLinkReturn.disabledMiddlewareReturn();

        // TODO

        return ChainLinkReturn.successMiddlewareReturn();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        String configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, LimitConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
