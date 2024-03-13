package com.networknt.aws.lambda.handler;

import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.status.Status;

public interface MiddlewareHandler extends LambdaHandler {

    String DISABLED_MIDDLEWARE_RETURN = "ERR14001";
    String SUCCESS_MIDDLEWARE_RETURN = "SUC14200";

    /**
     *
     * Indicate if this handler is enabled or not.
     *
     * @return boolean true if enabled
     */
    boolean isEnabled();

    /**
     * Register this handler to the handler registration.
     */
    void register();

    /**
     * Reload config values in case the config values changed by config server.
     */
    void reload();
    /**
     * Indicate if this middleware handler will continue on failure or not.
     */
    boolean isContinueOnFailure();

    /**
     * Indicate if this middleware handler is audited or not.
     */
    boolean isAudited();

    /**
     * Indicate if this middleware handler is asynchronous or not.
     */
    boolean isAsynchronous();

    Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException;

    void getCachedConfigurations();

    default Status disabledMiddlewareStatus() {
        return new Status(409, DISABLED_MIDDLEWARE_RETURN, "Middleware handler is disabled", "CONFLICT", "ERROR");
    }

    default Status successMiddlewareStatus() {
        return new Status(200, SUCCESS_MIDDLEWARE_RETURN, "OK", "SUCCESS", "SUCCESS");
    }

}
