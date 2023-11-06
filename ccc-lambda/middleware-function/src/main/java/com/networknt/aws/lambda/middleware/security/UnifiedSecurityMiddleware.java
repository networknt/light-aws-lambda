package com.networknt.aws.lambda.middleware.security;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnifiedSecurityMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(UnifiedSecurityMiddleware.class);
    private static final String CONFIG_NAME = "lambda-security";
    private static final SecurityConfig CONFIG = (SecurityConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, SecurityConfig.class);

    public UnifiedSecurityMiddleware() {
        super(false, false, false);
    }

    @Override
    protected Status executeMiddleware(LightLambdaExchange exchange) throws InterruptedException {
        return null;
    }

    @Override
    public void getCachedConfigurations() {

    }
}
