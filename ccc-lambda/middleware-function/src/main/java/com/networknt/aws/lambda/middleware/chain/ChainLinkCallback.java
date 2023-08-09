package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LightLambdaExchange;

public interface ChainLinkCallback {
    void callback(final LightLambdaExchange lambdaEventWrapper, ChainLinkReturn status);
    void exceptionCallback(final LightLambdaExchange lambdaEventWrapper, Throwable throwable);
}
