package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSynchronousFailedResponseMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousFailedResponseMiddleware(ChainLinkCallback callback, LambdaEventWrapper eventWrapper) {
        super(callback, eventWrapper, true, false, TestAsynchronousMiddleware.class);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {
        LOG.info("I am failing Synchronously");
        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED);
    }

}
