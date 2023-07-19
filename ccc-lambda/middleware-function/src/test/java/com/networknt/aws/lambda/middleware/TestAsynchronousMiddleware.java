package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

public class TestAsynchronousMiddleware extends LambdaMiddleware<String> {

    private static final Logger LOG = LoggerFactory.getLogger(TestAsynchronousMiddleware.class);

    public TestAsynchronousMiddleware(MiddlewareCallback callback, APIGatewayProxyRequestEvent input, LambdaContext context) {
        super(callback, input, context, TestAsynchronousMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<String> executeMiddleware() {
        LOG.info("I am executing asynchronously");

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
                LOG.error("Middleware exited on failure status!");
                LOG.error(e.getMessage(), e);
                return new MiddlewareReturn<>("Failed Asynchronous Response", MiddlewareReturn.Status.EXECUTION_FAILED);
            }
        }

        LOG.info("I am done executing asynchronously, doing callback");
        return new MiddlewareReturn<>("Success Asynchronous Response", MiddlewareReturn.Status.EXECUTION_SUCCESS);
    }

}

