package com.networknt.aws.lambda.middleware;
import com.networknt.aws.lambda.middleware.chain.Chainable;
import com.networknt.status.Status;

public abstract class LambdaMiddleware extends Chainable {
    public static final String DISABLED_MIDDLEWARE_RETURN = "ERR14001";
    public static final String SUCCESS_MIDDLEWARE_RETURN = "SUC14200";

    protected LambdaMiddleware(boolean audited, boolean asynchronous, boolean continueOnFailure) {
        super(audited, asynchronous, continueOnFailure);
    }

    protected abstract Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException;

    public abstract void getCachedConfigurations();
    public abstract boolean isEnabled();

    protected static Status disabledMiddlewareStatus() {
        return new Status(409, DISABLED_MIDDLEWARE_RETURN, "Middleware handler is disabled", "CONFLICT", "ERROR");
    }

    protected static Status successMiddlewareStatus() {
        return new Status(200, SUCCESS_MIDDLEWARE_RETURN, "OK", "SUCCESS", "SUCCESS");
    }
}
