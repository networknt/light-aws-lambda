package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class TestAsynchronousExceptionThrowingMiddleware extends LambdaMiddleware<String> {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestAsynchronousExceptionThrowingMiddleware(ChainLinkCallback callback, LambdaEventWrapper eventWrapper) {
        super(callback, eventWrapper, false, TestAsynchronousMiddleware.class);
    }

    @Override
    protected ChainLinkReturn<String> executeMiddleware() {
        LOG.info("I am failing Asynchronously");

        int randomSlept = ThreadLocalRandom.current().nextInt(5, 15);
        LOG.info("I will sleep a total of {} times", randomSlept);

        int slept = 0;
        while (slept < randomSlept) {
            try {
                int randomSleep = ThreadLocalRandom.current().nextInt(0, 1000);
                LOG.info("I am sleeping asynchronously for {}ms... ({})", randomSleep, slept);
                Thread.sleep(randomSleep);
                slept++;
            } catch (InterruptedException e) {
                // does not matter
            }
        }

        /* force throw an exception after some time. */
        throw new RuntimeException("I am throwing an exception asynchronously");
    }

}
