package com.networknt.aws.lambda.middleware.security;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.config.Config;
import com.networknt.security.SecurityConfig;
import com.networknt.status.Status;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwtVerifyMiddleware extends LambdaMiddleware implements MiddlewareHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SwtVerifyMiddleware.class);
    private static final SecurityConfig CONFIG = SecurityConfig.load(SecurityConfig.CONFIG_NAME);

    public SwtVerifyMiddleware() {
        super(false, false, false);
    }

    @Override
    protected Status executeMiddleware(LightLambdaExchange exchange) throws InterruptedException {
        return null;
    }

    @Override
    public void getCachedConfigurations() {
        // TODO
        throw new NotImplementedException();
    }

    @Override
    public boolean isEnabled() {

        // TODO - is this right?
        return CONFIG.isEnableVerifyJwt();
    }

    @Override
    public void register() {

    }

    @Override
    public void reload() {

    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        return null;
    }
}
