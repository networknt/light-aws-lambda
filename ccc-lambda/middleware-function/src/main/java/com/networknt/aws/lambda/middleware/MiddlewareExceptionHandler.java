package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;

public class MiddlewareExceptionHandler {

    final APIGatewayProxyRequestEvent proxyRequestEvent;
    final LambdaContext lambdaContext;
    final Throwable throwable;

    MiddlewareExceptionHandler(final APIGatewayProxyRequestEvent event, final LambdaContext context, Throwable t) {
        this.throwable = t;
        this.lambdaContext = context;
        this.proxyRequestEvent = event;
    }






}
