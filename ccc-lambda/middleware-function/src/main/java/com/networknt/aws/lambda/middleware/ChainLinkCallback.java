package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;

public interface ChainLinkCallback {
    void callback(final LambdaEventWrapper lambdaEventWrapper, ChainLinkReturn status);
    void exceptionCallback(final LambdaEventWrapper lambdaEventWrapper, Throwable throwable);
}
