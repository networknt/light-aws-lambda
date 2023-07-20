package com.networknt.aws.lambda.body;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ResponseBodyTransformerMiddleware extends LambdaMiddleware<Map<String, Object>> {

    Logger LOG = LoggerFactory.getLogger(ResponseBodyTransformerMiddleware.class);

    public ResponseBodyTransformerMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, RequestBodyTransformerMiddleware.class);
    }

    @Override
    protected ChainLinkReturn<Map<String, Object>> executeMiddleware() {
        return null;
    }
}
