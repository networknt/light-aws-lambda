package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;

public class MiddlewareRunnable implements Runnable {

    private final MiddlewareHandler middlewareHandler;
    private final LightLambdaExchange exchange;

    private final ChainLinkCallback callback;

    public MiddlewareRunnable(MiddlewareHandler middlewareHandler, LightLambdaExchange exchange, ChainLinkCallback callback) {
        this.middlewareHandler = middlewareHandler;
        this.exchange = exchange;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            var status = middlewareHandler.executeMiddleware(exchange);
            this.callback.callback(this.exchange, status);

        } catch (Throwable e) {
            this.callback.exceptionCallback(this.exchange, e);
        }

    }
}
