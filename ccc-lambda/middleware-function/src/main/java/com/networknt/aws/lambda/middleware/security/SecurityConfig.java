package com.networknt.aws.lambda.middleware.security;

public class SecurityConfig {

    boolean enableVerifyJwt;

    // Extract JWT scope token from the X-Scope-Token header and validate the JWT token
    boolean enableExtractScopeToken;

    // Enable JWT scope verification.
    // Only valid when (enableVerifyJwt is true) AND (enableVerifyJWTScopeToken is true)
    boolean enableVerifyScope;

    // User for test only. should be always be false on official environment.
    boolean enableMockJwt;

    // For test only, should be always be true on official environment.
    boolean ignoreJwtExpiry;

    // Enable or disable JWT token logging
    boolean logJwtToken;

    // Enable or disable client_id, user_id and scope logging.
    boolean logClientUserScope;

    // Enable JWT token cache to speed up verification. This will only verify expired time
    // and skip the signature verification as it takes more CPU power and long time.
    boolean enableJwtCache;

    public boolean getIgnoreJwtExpiry() {
        return ignoreJwtExpiry;
    }

    public boolean isEnableVerifyJwt() {
        return enableVerifyJwt;
    }

    public boolean isEnableExtractScopeToken() {
        return enableExtractScopeToken;
    }

    public boolean isEnableVerifyScope() {
        return enableVerifyScope;
    }

    public boolean isEnableMockJwt() {
        return enableMockJwt;
    }

    public boolean isIgnoreJwtExpiry() {
        return ignoreJwtExpiry;
    }

    public boolean isLogJwtToken() {
        return logJwtToken;
    }

    public boolean isLogClientUserScope() {
        return logClientUserScope;
    }

    public boolean isEnableJwtCache() {
        return enableJwtCache;
    }
}
