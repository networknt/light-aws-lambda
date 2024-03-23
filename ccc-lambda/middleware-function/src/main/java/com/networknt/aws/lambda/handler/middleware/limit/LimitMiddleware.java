package com.networknt.aws.lambda.handler.middleware.limit;


import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.handler.middleware.header.HeaderMiddleware;
import com.networknt.config.Config;
import com.networknt.header.HeaderConfig;
import com.networknt.limit.LimitConfig;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitMiddleware implements MiddlewareHandler {
    private static final Logger LOG = LoggerFactory.getLogger(LimitMiddleware.class);
    private static LimitConfig CONFIG = LimitConfig.load();

    public LimitMiddleware() {
        if (LOG.isInfoEnabled()) LOG.info("LimitMiddleware is constructed");
        CONFIG = LimitConfig.load();
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
        ModuleRegistry.registerModule(
                LimitConfig.CONFIG_NAME,
                LimitMiddleware.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(LimitConfig.CONFIG_NAME),
                null
        );
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
