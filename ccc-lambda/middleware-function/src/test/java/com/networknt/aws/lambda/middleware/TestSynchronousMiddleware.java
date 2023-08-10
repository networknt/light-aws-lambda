package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.status.LambdaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class TestSynchronousMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousMiddleware(ChainLinkCallback callback, LightLambdaExchange eventWrapper) {
        super(true, false, false, callback, eventWrapper);
    }

    @Override
    protected LambdaStatus executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
        LOG.info("I am executing Synchronously");

        int randomSlept = ThreadLocalRandom.current().nextInt(5, 15);
        LOG.info("I will sleep a total of {} times", randomSlept);

        int slept = 0;
        while (slept < randomSlept) {
            LOG.info("I am working Synchronously... ({})", slept);
            Thread.sleep(150);
            slept++;
        }

        LOG.info("I am done executing Synchronously, doing callback");
        return LambdaStatus.successMiddlewareReturn();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {

    }

}
