package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAsynchronousMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TestAsynchronousMiddleware.class);

    public TestAsynchronousMiddleware(MiddlewareCallback callback, APIGatewayProxyRequestEvent input, LambdaContext context) {
        super(callback, input, context, TestAsynchronousMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<String> executeMiddleware() {
        LOG.info("I am executing asynchronously");

        int slept = 0;
        while (slept < 5) {
            try {
                LOG.info("I am working asynchronously... ({})", slept);
                Thread.sleep(4);
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

