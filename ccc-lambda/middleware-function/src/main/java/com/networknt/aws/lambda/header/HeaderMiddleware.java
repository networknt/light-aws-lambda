package com.networknt.aws.lambda.header;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderMiddleware extends LambdaMiddleware {

    public static final String CONFIG_NAME = "header";

    static final HeaderConfig config = (HeaderConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HeaderConfig.class);

    private static final Logger LOG = LoggerFactory.getLogger(HeaderMiddleware.class);

    public HeaderMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, true, HeaderMiddleware.class);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() {


        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_SUCCESS);
    }
}
