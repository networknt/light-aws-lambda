package com.networknt.aws.lambda.middleware.correlation;

public class CorrelationConfig {
    boolean enabled;
    boolean autogenCorrelationID;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAutogenCorrelationID() {
        return autogenCorrelationID;
    }
}
