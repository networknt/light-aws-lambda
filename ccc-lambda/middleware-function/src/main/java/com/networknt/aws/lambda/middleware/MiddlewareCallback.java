package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;

public interface MiddlewareCallback {
    void callback(final LambdaEventWrapper lambdaEventWrapper, MiddlewareReturn<?> status);
    void exceptionCallback(final LambdaEventWrapper lambdaEventWrapper, Throwable throwable);
}
