package com.networknt.aws.lambda.body;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.MiddlewareCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RequestBodyTransformerMiddleware extends LambdaMiddleware<Map<String, Object>> {

    private static final Logger LOG = LoggerFactory.getLogger(RequestBodyTransformerMiddleware.class);

    public RequestBodyTransformerMiddleware(MiddlewareCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, RequestBodyTransformerMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<Map<String, Object>> executeMiddleware() {
        return null;
    }
}
