package com.networknt.aws.lambda.middleware.traceability;

public class TraceabilityConfig {

    boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
