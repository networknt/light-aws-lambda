package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

@ChainProperties(asynchronous = true, chainId = "TestAsynchronousExceptionThrowingMiddleware")
public class TestAsynchronousExceptionThrowingMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestAsynchronousExceptionThrowingMiddleware(ChainLinkCallback callback, LambdaEventWrapper eventWrapper) {
        super(callback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {
        LOG.info("I am failing Asynchronously");

        int randomSlept = ThreadLocalRandom.current().nextInt(1, 3);
        LOG.info("I will sleep a total of {} times", randomSlept);

        int slept = 0;
        while (slept < randomSlept) {
            int randomSleep = ThreadLocalRandom.current().nextInt(0, 500);
            LOG.info("I am sleeping asynchronously for {}ms... ({})", randomSleep, slept);
            Thread.sleep(randomSleep);
            slept++;
        }

        /* force throw an exception after some time. */
        throw new RuntimeException("I am throwing an exception asynchronously");
    }

}
