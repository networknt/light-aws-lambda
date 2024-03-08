package com.networknt.aws.lambda.handler;

public interface MiddlewareHandler extends LambdaHandler {
    /**
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

}
