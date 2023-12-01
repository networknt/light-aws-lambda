package com.networknt.aws.lambda.middleware.limit;


import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.limit.LimitConfig;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);
    private static final  LimitConfig CONFIG = LimitConfig.load();

    public LimitMiddleware() {
        super(false, true, false);
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
