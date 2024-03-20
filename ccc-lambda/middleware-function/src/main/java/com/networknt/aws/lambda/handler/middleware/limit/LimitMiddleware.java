package com.networknt.aws.lambda.handler.middleware.limit;


import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.limit.LimitConfig;
import com.networknt.status.Status;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitMiddleware implements MiddlewareHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LimitMiddleware.class);
    private static final  LimitConfig CONFIG = LimitConfig.load();

    public LimitMiddleware() {
        if (LOG.isInfoEnabled()) LOG.info("LimitMiddleware is constructed");
    }

    @Override
    public Status execute(final LightLambdaExchange exchange) throws InterruptedException {
        throw new NotImplementedException();
    }

    @Override
    public void getCachedConfigurations() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isEnabled() {
        return CONFIG.isEnabled();
    }

    @Override
    public void register() {
        throw new NotImplementedException();
    }

    @Override
    public void reload() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isContinueOnFailure() {
        return false;
    }

    @Override
    public boolean isAudited() {
        return false;
    }

    @Override
    public boolean isAsynchronous() {
        return false;
    }
}
