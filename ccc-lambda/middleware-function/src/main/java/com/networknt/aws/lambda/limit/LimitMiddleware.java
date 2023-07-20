package com.networknt.aws.lambda.limit;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LimitMiddleware extends LambdaMiddleware<Long> {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);

    public LimitMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, false, LimitMiddleware.class);
    }

    @Override
    protected ChainLinkReturn<Long> executeMiddleware() {
        return null;
    }
}
