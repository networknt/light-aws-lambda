package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestSynchronousFailedResponseMiddleware extends LambdaMiddleware<String> {
    private static final Logger LOG = LoggerFactory.getLogger(TestSynchronousMiddleware.class);

    public TestSynchronousFailedResponseMiddleware(MiddlewareCallback callback, APIGatewayProxyRequestEvent input, LambdaContext context) {
        super(callback, input, context, true, TestAsynchronousMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<String> executeMiddleware() {
        LOG.info("I am failing Synchronously");
        return new MiddlewareReturn<>("Failed response", MiddlewareReturn.Status.EXECUTION_FAILED);
    }

}
