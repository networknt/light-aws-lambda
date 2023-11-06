package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LambdaMiddleware extends Chainable {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaMiddleware.class);
    public static final String DISABLED_MIDDLEWARE_RETURN = "ERR14001";
    public static final String SUCCESS_MIDDLEWARE_RETURN = "SUC14200";
    public boolean hasFailure = false;
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public LambdaMiddleware(boolean audited, boolean asynchronous, boolean continueOnFailure) {
        super(audited, asynchronous, continueOnFailure);
    }

    protected abstract Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException;

    public abstract void getCachedConfigurations();

    protected static Status disabledMiddlewareStatus() {
        return new Status(DISABLED_MIDDLEWARE_RETURN);
    }

    protected static Status successMiddlewareStatus() {
        return new Status(200, SUCCESS_MIDDLEWARE_RETURN, "OK", "SUCCESS", "SUCCESS");
    }
}
