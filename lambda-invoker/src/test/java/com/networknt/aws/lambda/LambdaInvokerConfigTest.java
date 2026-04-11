package com.networknt.aws.lambda;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class LambdaInvokerConfigTest {
    private static LambdaInvokerConfig config = (LambdaInvokerConfig) Config.getInstance().getJsonObjectConfig(LambdaInvokerConfig.CONFIG_NAME, LambdaInvokerConfig.class);

    @Test
    public void testFunctionMapping() {
        Map<String, String> functions = config.getFunctions();
        System.out.println(JsonMapper.toJson(functions));
    }

    @Test
    public void testStsTypeWithoutRoleArnThrowsConfigException() {
        assertThrows(ConfigException.class, () -> LambdaInvokerConfig.load("lambda-invoker-sts-no-role"),
                "ConfigException was not thrown despite stsType defined with blank roleArn");
    }

    @Test
    public void testStsTypeWithRoleArnSucceeds() {
        LambdaInvokerConfig stsConfig = LambdaInvokerConfig.load("lambda-invoker-sts-with-role");
        assertNotNull(stsConfig);
        assertEquals("arn:aws:iam::123456789012:role/TestRole", stsConfig.getRoleArn());
    }

    @Test
    public void testStsTypeFuncWithRoleArn() {
        LambdaInvokerConfig stsConfig = LambdaInvokerConfig.load("lambda-invoker-sts-func");
        assertNotNull(stsConfig);
        assertEquals("arn:aws:iam::123456789012:role/TestRole", stsConfig.getRoleArn());
    }

    @Test
    public void testInvalidStsTypeThrowsConfigException() {
        ConfigException ex = assertThrows(ConfigException.class,
                () -> LambdaInvokerConfig.load("lambda-invoker-sts-invalid-type"),
                "ConfigException was not thrown for unsupported stsType value");
        assertTrue(ex.getMessage().contains("stsType"), "Exception message should mention stsType");
    }

    @Test
    public void testTokenRefreshDecisionWhenAuthorizationMissing() {
        AtomicReference<String> cache = new AtomicReference<>("token1");

        assertFalse(shouldRefreshStsWebIdentityToken(cache, null),
                "Missing Authorization should not trigger a refresh");
        assertEquals("token1", cache.get(), "Cached token should remain unchanged when Authorization is missing");
    }

    @Test
    public void testTokenRefreshDecisionWhenTokenUnchanged() {
        AtomicReference<String> cache = new AtomicReference<>("token2");

        assertFalse(shouldRefreshStsWebIdentityToken(cache, "Bearer token2"),
                "Same token should use the cached fast-path and avoid refresh");
        assertEquals("token2", cache.get(), "Cached token should remain unchanged when the token matches");
    }

    @Test
    public void testTokenRefreshDecisionWhenTokenChanges() {
        AtomicReference<String> cache = new AtomicReference<>("token2");

        assertTrue(shouldRefreshStsWebIdentityToken(cache, "Bearer token3"),
                "Different token should trigger a refresh");
        assertEquals("token3", cache.get(), "Cached token should be updated after a token change");
    }

    private static boolean shouldRefreshStsWebIdentityToken(AtomicReference<String> cache, String authorization) {
        String incomingToken = extractAuthorizationToken(authorization);
        if (incomingToken == null || incomingToken.isBlank()) {
            return false;
        }

        String cachedToken = cache.get();
        if (incomingToken.equals(cachedToken)) {
            return false;
        }

        cache.set(incomingToken);
        return true;
    }

    private static String extractAuthorizationToken(String authorization) {
        if (authorization == null) {
            return null;
        }

        String prefix = "Bearer ";
        return authorization.startsWith(prefix) ? authorization.substring(prefix.length()) : authorization;
    }
}
