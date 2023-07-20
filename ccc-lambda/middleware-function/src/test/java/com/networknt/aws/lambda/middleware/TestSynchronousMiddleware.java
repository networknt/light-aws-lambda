package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class TestSynchronousMiddleware extends LambdaMiddleware<String> {

    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousMiddleware(ChainLinkCallback callback, LambdaEventWrapper eventWrapper) {
        super(callback, eventWrapper, true, TestAsynchronousMiddleware.class);
    }

    @Override
    protected ChainLinkReturn<String> executeMiddleware() {
        LOG.info("I am executing Synchronously");

        int randomSlept = ThreadLocalRandom.current().nextInt(5, 15);
        LOG.info("I will sleep a total of {} times", randomSlept);

        int slept = 0;
        while (slept < randomSlept) {
            try {
                LOG.info("I am working Synchronously... ({})", slept);
                Thread.sleep(150);
                slept++;
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
                return new ChainLinkReturn<>("Failed Synchronous Response", ChainLinkReturn.Status.EXECUTION_FAILED);
            }
        }

        LOG.info("I am done executing Synchronously, doing callback");
        return new ChainLinkReturn<>("Success Synchronous Response", ChainLinkReturn.Status.EXECUTION_SUCCESS);
    }

}
