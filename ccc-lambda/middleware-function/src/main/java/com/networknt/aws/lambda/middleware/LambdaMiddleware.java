package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LambdaMiddleware extends Chainable implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);

    private final LightLambdaExchange exchange;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public LambdaMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange exchange) {
        super(middlewareCallback);
        this.exchange = exchange;
    }

    protected abstract ChainLinkReturn executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException;

    public abstract void getAppConfigProfileConfigurations(String applicationId, String env);

    @Override
    public void run() {

        try {

            var status = this.executeMiddleware(exchange);
            LOG.debug("Middleware '{}' returned.", status.toStringConditionally(true, true, true));
            this.middlewareCallback.callback(this.exchange, status);

        } catch (Throwable e) {
            LOG.error("Middleware ended with exception", e);
            this.middlewareCallback.exceptionCallback(this.exchange, e);
        }

    }
}
