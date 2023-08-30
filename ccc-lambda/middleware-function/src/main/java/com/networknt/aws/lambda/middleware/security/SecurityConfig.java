package com.networknt.aws.lambda.middleware.security;

import java.util.List;
import java.util.Map;

public class SecurityConfig {
    public static final String CONFIG_NAME = "lambda-security";

    public static final String CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";

    boolean enableVerifyJwt;

    boolean enableVerifySwt;

    String swtClientIdHeader = "swt-client";

    String swtClientSecretHeader = "swt-secret";

    // Extract JWT scope token from the X-Scope-Token header and validate the JWT token
    boolean enableExtractScopeToken;

    // Enable JWT scope verification.
    // Only valid when (enableVerifyJwt is true) AND (enableVerifyJWTScopeToken is true)
    boolean enableVerifyScope;

    boolean skipVerifyScopeWithoutSpec;

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

    int jwtCacheFullSize;

    boolean bootstrapFromKeyService;

    List<String> skipPathPrefixes;

    Map<String, String> passThroughClaims;
    Map<String, Object> jwt;

    public boolean getIgnoreJwtExpiry() {
        return ignoreJwtExpiry;
    }

    public boolean isEnableVerifyJwt() {
        return enableVerifyJwt;
    }

    public boolean isEnableVerifySwt() {
        return enableVerifySwt;
    }

    public String getSwtClientIdHeader() {
        return swtClientIdHeader;
    }

    public String getSwtClientSecretHeader() {
        return swtClientSecretHeader;
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

    public boolean isSkipVerifyScopeWithoutSpec() {
        return skipVerifyScopeWithoutSpec;
    }

    public int getJwtCacheFullSize() {
        return jwtCacheFullSize;
    }

    public boolean isBootstrapFromKeyService() {
        return bootstrapFromKeyService;
    }

    public List<String> getSkipPathPrefixes() {
        return skipPathPrefixes;
    }

    public Map<String, String> getPassThroughClaims() {
        return passThroughClaims;
    }

    public Map<String, Object> getJwt() {
        return jwt;
    }
}
