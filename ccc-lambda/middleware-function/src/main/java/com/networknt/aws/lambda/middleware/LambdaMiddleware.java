package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LambdaMiddleware extends Chainable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);
    private final LightLambdaExchange exchange;
    public static final String DISABLED_MIDDLEWARE_RETURN = "ERR14001";
    public static final String SUCCESS_MIDDLEWARE_RETURN = "SUC14200";
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public LambdaMiddleware(boolean audited, boolean asynchronous, boolean continueOnFailure, ChainLinkCallback middlewareCallback, final LightLambdaExchange exchange) {
        super(audited, asynchronous, continueOnFailure, middlewareCallback);
        this.exchange = exchange;
    }

    protected abstract Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException;

    public abstract void getAppConfigProfileConfigurations(String applicationId, String env);

    @Override
    public void run() {

        try {

            var status = this.executeMiddleware(exchange);
            LOG.debug("Middleware returned with status: '{}'.", status.toStringConditionally());
            this.middlewareCallback.callback(this.exchange, status);

        } catch (Throwable e) {
            LOG.error("Middleware ended with exception", e);
            this.middlewareCallback.exceptionCallback(this.exchange, e);
        }

    }

    protected static Status disabledMiddlewareStatus() {
        return new Status(DISABLED_MIDDLEWARE_RETURN);
    }

    protected static Status successMiddlewareStatus() {
        return new Status(200, SUCCESS_MIDDLEWARE_RETURN, "OK", "SUCCESS", "SUCCESS");
    }
}
