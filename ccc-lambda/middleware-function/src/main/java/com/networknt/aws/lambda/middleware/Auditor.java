package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;

public class Auditor {

    final LambdaEventWrapper eventWrapper;

    public Auditor(LambdaEventWrapper eventWrapper) {
        this.eventWrapper = eventWrapper;
    }



}
