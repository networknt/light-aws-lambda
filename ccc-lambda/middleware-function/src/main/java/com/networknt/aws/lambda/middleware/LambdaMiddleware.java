package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;

public abstract class LambdaMiddleware extends Chainable implements Runnable {
    final ChainLinkCallback middlewareCallback;
    protected final LambdaEventWrapper eventWrapper;

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public LambdaMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper, final boolean isSynchronous, final boolean isAudited, Class<? extends LambdaMiddleware> chainableId) {
        super(chainableId.getName(), isSynchronous, isAudited);
        this.middlewareCallback = middlewareCallback;
        this.eventWrapper = eventWrapper;
    }

    protected abstract ChainLinkReturn executeMiddleware();

    @Override
    public void run() {
        try {
            var status = this.executeMiddleware();
            this.middlewareCallback.callback(this.eventWrapper, status);

        } catch (Throwable e) {
            this.middlewareCallback.exceptionCallback(this.eventWrapper, e);
        }

    }
}
