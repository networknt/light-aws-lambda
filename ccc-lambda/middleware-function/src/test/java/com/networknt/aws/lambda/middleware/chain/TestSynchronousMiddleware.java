package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class TestSynchronousMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousMiddleware() {
        super(true, false, false);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
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
        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {

    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
