package com.networknt.aws.lambda.middleware.validator;

import java.util.List;

public class ValidatorConfig {

    private boolean enabled;
    private boolean logError;
    private boolean validateResponse;
    private boolean validateRequest;
    private boolean handleNullableField;
    private List<String> skipPathPrefixes;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isLogError() {
        return logError;
    }

    public boolean isValidateRequest() {
        return validateRequest;
    }

    public boolean isValidateResponse() {
        return validateResponse;
    }

    public boolean isHandleNullableField() {
        return handleNullableField;
    }

    public List<String> getSkipPathPrefixes() {
        return skipPathPrefixes;
    }
}
