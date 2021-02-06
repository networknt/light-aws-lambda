package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.*;

public class LambdaContext implements Context {

    private String requestId;

    public LambdaContext(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String getAwsRequestId() {
        return requestId;
    }

    @Override
    public String getLogGroupName() {
        return System.getenv("AWS_LAMBDA_LOG_GROUP_NAME");
    }

    @Override
    public String getLogStreamName() {
        return System.getenv("AWS_LAMBDA_LOG_STREAM_NAME");
    }

    @Override
    public String getFunctionName() {
        return System.getenv("AWS_LAMBDA_FUNCTION_NAME");
    }

    @Override
    public String getFunctionVersion() {
        return System.getenv("AWS_LAMBDA_FUNCTION_VERSION");
    }

    @Override
    public String getInvokedFunctionArn() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CognitoIdentity getIdentity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientContext getClientContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getRemainingTimeInMillis() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMemoryLimitInMB() {
        return Integer.parseInt(System.getenv("AWS_LAMBDA_FUNCTION_MEMORY_SIZE"));
    }

    @Override
    public LambdaLogger getLogger() {
        return LambdaRuntime.getLogger();
    }
}
