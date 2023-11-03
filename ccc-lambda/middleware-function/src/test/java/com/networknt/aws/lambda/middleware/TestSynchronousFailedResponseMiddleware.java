package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSynchronousFailedResponseMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousFailedResponseMiddleware(ChainLinkCallback callback) {
        super(true, false, false, callback);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
        LOG.info("I am failing Synchronously");
        return new Status("ERR14004");
    }


    @Override
    public void getCachedConfigurations() {

    }
}
