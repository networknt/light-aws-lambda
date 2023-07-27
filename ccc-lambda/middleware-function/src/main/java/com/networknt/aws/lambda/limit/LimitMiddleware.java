package com.networknt.aws.lambda.limit;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChainProperties(asynchronous = true, id = "LimitMiddleware", audited = false)
public class LimitMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);

    public LimitMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {
        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_SUCCESS);
    }
}
