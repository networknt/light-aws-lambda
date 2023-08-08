package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChainProperties()
public class TestSynchronousFailedResponseMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousFailedResponseMiddleware(ChainLinkCallback callback, LambdaEventWrapper eventWrapper) {
        super(callback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {
        LOG.info("I am failing Synchronously");
        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, "ERR14004");
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {

    }

}
