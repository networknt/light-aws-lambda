package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;

public abstract class LambdaMiddleware extends Chainable implements Runnable {
    final MiddlewareCallback middlewareCallback;
    final APIGatewayProxyRequestEvent proxyRequestEvent;
    final Context context;

    public LambdaMiddleware(MiddlewareCallback middlewareCallback, final APIGatewayProxyRequestEvent input, final Context context, final boolean isSynchronous, Class<? extends LambdaMiddleware> chainableId) {
        super(chainableId.getName(), isSynchronous);
        this.middlewareCallback = middlewareCallback;
        this.proxyRequestEvent = input;
        this.context = context;
    }

    public LambdaMiddleware(MiddlewareCallback middlewareCallback, final APIGatewayProxyRequestEvent input, final Context context, Class<? extends LambdaMiddleware> chainableId) {
        super(chainableId.getName(), false);
        this.middlewareCallback = middlewareCallback;
        this.proxyRequestEvent = input;
        this.context = context;
    }

    protected abstract MiddlewareReturn executeMiddleware();

    public void run() {
        var status = this.executeMiddleware();
        this.middlewareCallback.callback(this.proxyRequestEvent, this.context, status);
    }
}
