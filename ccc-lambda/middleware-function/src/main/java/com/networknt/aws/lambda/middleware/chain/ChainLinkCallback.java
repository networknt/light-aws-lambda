package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.status.Status;

public interface ChainLinkCallback {
    void callback(final LightLambdaExchange lambdaEventWrapper, Status status);
    void exceptionCallback(final LightLambdaExchange lambdaEventWrapper, Throwable throwable);
}
