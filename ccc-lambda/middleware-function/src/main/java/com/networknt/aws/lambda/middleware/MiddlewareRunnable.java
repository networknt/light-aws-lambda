package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;

public class MiddlewareRunnable implements Runnable {

    private final LambdaMiddleware lambdaMiddleware;
    private final LightLambdaExchange exchange;

    private final ChainLinkCallback callback;

    public MiddlewareRunnable(LambdaMiddleware middleware, LightLambdaExchange exchange, ChainLinkCallback callback) {
        this.lambdaMiddleware = middleware;
        this.exchange = exchange;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            var status = lambdaMiddleware.executeMiddleware(exchange);
            this.callback.callback(this.exchange, status);

        } catch (Throwable e) {
            this.callback.exceptionCallback(this.exchange, e);
        }

    }
}
