package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.status.LambdaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSynchronousFailedResponseMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousFailedResponseMiddleware(ChainLinkCallback callback, LightLambdaExchange eventWrapper) {
        super(true, false, false, callback, eventWrapper);
    }

    @Override
    protected LambdaStatus executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
        LOG.info("I am failing Synchronously");
        return new LambdaStatus(LambdaStatus.Status.EXECUTION_FAILED, "ERR14004");
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {

    }

}
