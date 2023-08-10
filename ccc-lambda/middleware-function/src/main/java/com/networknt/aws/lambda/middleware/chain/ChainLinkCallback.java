package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.status.LambdaStatus;

public interface ChainLinkCallback {
    void callback(final LightLambdaExchange lambdaEventWrapper, LambdaStatus status);
    void exceptionCallback(final LightLambdaExchange lambdaEventWrapper, Throwable throwable);
}
