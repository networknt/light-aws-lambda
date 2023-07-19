package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;

public abstract class LambdaMiddleware<T> extends Chainable implements Runnable {
    final MiddlewareCallback middlewareCallback;
    protected final LambdaEventWrapper eventWrapper;

    public LambdaMiddleware(MiddlewareCallback middlewareCallback, final LambdaEventWrapper eventWrapper, final boolean isSynchronous, Class<? extends LambdaMiddleware<?>> chainableId) {
        super(chainableId.getName(), isSynchronous);
        this.middlewareCallback = middlewareCallback;
        this.eventWrapper = eventWrapper;
    }

    public LambdaMiddleware(MiddlewareCallback middlewareCallback, final LambdaEventWrapper eventWrapper, Class<? extends LambdaMiddleware<?>> chainableId) {
        super(chainableId.getName(), false);
        this.middlewareCallback = middlewareCallback;
        this.eventWrapper = eventWrapper;
    }

    protected abstract MiddlewareReturn<T> executeMiddleware();

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
