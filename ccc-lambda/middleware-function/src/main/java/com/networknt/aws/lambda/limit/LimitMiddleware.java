package com.networknt.aws.lambda.limit;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.MiddlewareCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitMiddleware extends LambdaMiddleware<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);

    public LimitMiddleware(MiddlewareCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, false, LimitMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<Long> executeMiddleware() {
        return null;
    }
}
