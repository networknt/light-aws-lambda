package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;

public abstract class LambdaMiddleware<T> extends Chainable implements Runnable {
    final MiddlewareCallback middlewareCallback;
    protected final APIGatewayProxyRequestEvent proxyRequestEvent;
    protected final LambdaContext context;

    public LambdaMiddleware(MiddlewareCallback middlewareCallback, final APIGatewayProxyRequestEvent input, final LambdaContext context, final boolean isSynchronous, Class<? extends LambdaMiddleware> chainableId) {
        super(chainableId.getName(), isSynchronous);
        this.middlewareCallback = middlewareCallback;
        this.proxyRequestEvent = input;
        this.context = context;
    }

    public LambdaMiddleware(MiddlewareCallback middlewareCallback, final APIGatewayProxyRequestEvent input, final LambdaContext context, Class<? extends LambdaMiddleware> chainableId) {
        super(chainableId.getName(), false);
        this.middlewareCallback = middlewareCallback;
        this.proxyRequestEvent = input;
        this.context = context;
    }

    protected abstract MiddlewareReturn<T> executeMiddleware();

    @Override
    public void run() {
        var status = this.executeMiddleware();
        this.middlewareCallback.callback(this.proxyRequestEvent, this.context, status);
    }
}
