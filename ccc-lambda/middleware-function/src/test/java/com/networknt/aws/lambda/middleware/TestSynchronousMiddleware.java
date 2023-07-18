package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSynchronousMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousMiddleware(MiddlewareCallback callback, APIGatewayProxyRequestEvent input, LambdaContext context) {
        super(callback, input, context, true, TestAsynchronousMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<String> executeMiddleware() {
        LOG.info("I am executing Synchronously");

        int slept = 0;
        while (slept < 5) {
            try {
                LOG.info("I am working Synchronously... ({})", slept);
                Thread.sleep(4);
                slept++;
            } catch (InterruptedException e) {
                LOG.error(e.getMessage(), e);
                return new MiddlewareReturn<>("Failed Synchronous Response", MiddlewareReturn.Status.EXECUTION_FAILED);
            }
        }

        LOG.info("I am done executing Synchronously, doing callback");
        return new MiddlewareReturn<>("Success Synchronous Response", MiddlewareReturn.Status.EXECUTION_SUCCESS);
    }

}
