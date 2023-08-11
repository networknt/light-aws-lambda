package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.status.LambdaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LambdaMiddleware extends Chainable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);

    private final LightLambdaExchange exchange;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public LambdaMiddleware(boolean audited, boolean asynchronous, boolean continueOnFailure, ChainLinkCallback middlewareCallback, final LightLambdaExchange exchange) {
        super(audited, asynchronous, continueOnFailure, middlewareCallback);
        this.exchange = exchange;
    }

    protected abstract LambdaStatus executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException;

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
}
