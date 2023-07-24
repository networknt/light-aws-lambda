package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class TestAsynchronousMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TestAsynchronousMiddleware.class);

    public TestAsynchronousMiddleware(ChainLinkCallback callback, LambdaEventWrapper eventWrapper) {
        super(callback, eventWrapper, false, false, TestAsynchronousMiddleware.class);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {
        LOG.info("I am executing asynchronously");

        int randomSlept = ThreadLocalRandom.current().nextInt(5, 15);
        LOG.info("I will sleep a total of {} times", randomSlept);

        int slept = 0;
        while (slept < randomSlept) {
            int randomSleep = ThreadLocalRandom.current().nextInt(0, 1000);
            LOG.info("I am sleeping asynchronously for {}ms... ({})", randomSleep, slept);
            Thread.sleep(randomSleep);
            slept++;
        }

        LOG.info("I am done executing asynchronously, doing callback");
        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_SUCCESS);
    }

}

