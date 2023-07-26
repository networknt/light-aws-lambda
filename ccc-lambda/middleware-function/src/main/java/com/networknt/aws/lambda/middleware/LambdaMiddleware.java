package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LambdaMiddleware extends Chainable implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);

    protected final LambdaEventWrapper eventWrapper;
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public LambdaMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback);
        this.eventWrapper = eventWrapper;
    }

    protected abstract ChainLinkReturn executeMiddleware() throws InterruptedException;

    @Override
    public void run() {
        try {
            var status = this.executeMiddleware();

            if (LOG.isTraceEnabled())
                LOG.trace("status '{}' returned", status.getStatus());

            this.middlewareCallback.callback(this.eventWrapper, status);

        } catch (Throwable e) {
            LOG.error("Middleware ended with exception", e);
            this.middlewareCallback.exceptionCallback(this.eventWrapper, e);
        }

    }
}
