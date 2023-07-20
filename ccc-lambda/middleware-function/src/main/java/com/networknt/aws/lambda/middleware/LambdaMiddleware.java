package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;

public abstract class LambdaMiddleware<T> extends Chainable implements Runnable {
    final ChainLinkCallback middlewareCallback;
    protected final LambdaEventWrapper eventWrapper;

    public LambdaMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper, final boolean isSynchronous, Class<? extends LambdaMiddleware<?>> chainableId) {
        super(chainableId.getName(), isSynchronous);
        this.middlewareCallback = middlewareCallback;
        this.eventWrapper = eventWrapper;
    }

    public LambdaMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper, Class<? extends LambdaMiddleware<?>> chainableId) {
        super(chainableId.getName(), false);
        this.middlewareCallback = middlewareCallback;
        this.eventWrapper = eventWrapper;
    }

    protected abstract ChainLinkReturn<T> executeMiddleware();

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
