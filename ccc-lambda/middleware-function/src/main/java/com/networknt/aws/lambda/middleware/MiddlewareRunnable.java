package com.networknt.aws.lambda.middleware;

public class MiddlewareRunnable implements Runnable {

    private final LambdaMiddleware lambdaMiddleware;
    private final LightLambdaExchange exchange;

    public MiddlewareRunnable(LambdaMiddleware middleware, LightLambdaExchange exchange) {
        this.lambdaMiddleware = middleware;
        this.exchange = exchange;
    }

    @Override
    public void run() {
        try {
            var status = lambdaMiddleware.executeMiddleware(exchange);
            lambdaMiddleware.middlewareCallback.callback(this.exchange, status);

        } catch (Throwable e) {
            lambdaMiddleware.middlewareCallback.exceptionCallback(this.exchange, e);
        }

    }
}
