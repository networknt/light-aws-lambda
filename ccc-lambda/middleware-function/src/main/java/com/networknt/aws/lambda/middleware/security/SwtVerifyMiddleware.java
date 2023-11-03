package com.networknt.aws.lambda.middleware.security;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwtVerifyMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(SwtVerifyMiddleware.class);
    private static final String CONFIG_NAME = "lambda-security";
    private static final SecurityConfig CONFIG = (SecurityConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, SecurityConfig.class);

    public SwtVerifyMiddleware(ChainLinkCallback middlewareCallback) {
        super(false, false, false, middlewareCallback);
    }

    @Override
    protected Status executeMiddleware(LightLambdaExchange exchange) throws InterruptedException {
        return null;
    }

    @Override
    public void getCachedConfigurations() {

    }


}
