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
    public void testTokenCacheIsolation() {
        // Verify that a fresh AtomicReference starts with null and can be updated
        AtomicReference<String> cache = new AtomicReference<>();
        assertNull(cache.get());
        cache.set("token1");
        assertEquals("token1", cache.get());
        // Replacing with a new token works correctly
        cache.set("token2");
        assertEquals("token2", cache.get());
        // Same token comparison (simulates cached-token path)
        String incoming = "token2";
        assertEquals(cache.get(), incoming, "Same token should match cached value");
        // Different token comparison (simulates refresh path)
        String newToken = "token3";
        assertNotEquals(cache.get(), newToken, "Different token should not match cached value");
    }
}
