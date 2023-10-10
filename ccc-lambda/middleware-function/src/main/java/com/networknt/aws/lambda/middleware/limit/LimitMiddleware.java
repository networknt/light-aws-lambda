package com.networknt.aws.lambda.middleware.limit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.cache.CachedConfig;
import com.networknt.aws.lambda.cache.LambdaCache;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.header.HeaderConfig;
import com.networknt.aws.lambda.middleware.validator.ValidatorConfig;
import com.networknt.aws.lambda.middleware.validator.ValidatorMiddleware;
import com.networknt.aws.lambda.proxy.LambdaProxy;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);
    private static final String CONFIG_NAME = "lambda-limit";
    private static LimitConfig CONFIG = (LimitConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LimitConfig.class);

    public LimitMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange eventWrapper) {
        super(false, true, false, middlewareCallback, eventWrapper);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        // TODO

        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {
    }
}
