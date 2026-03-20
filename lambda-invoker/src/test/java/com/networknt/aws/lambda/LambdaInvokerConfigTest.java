package com.networknt.aws.lambda;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LambdaInvokerConfigTest {
    private static LambdaInvokerConfig config = (LambdaInvokerConfig) Config.getInstance().getJsonObjectConfig(LambdaInvokerConfig.CONFIG_NAME, LambdaInvokerConfig.class);

    @Test
    public void testFunctionMapping() {
        Map<String, String> functions = config.getFunctions();
        System.out.println(JsonMapper.toJson(functions));
    }

    @Test
    public void testStsEnabledWithoutRoleArnThrowsConfigException() {
        assertThrows(ConfigException.class, () -> LambdaInvokerConfig.load("lambda-invoker-sts-no-role"),
                "ConfigException was not thrown despite stsEnabled=true with blank roleArn");
    }

    @Test
    public void testStsEnabledWithRoleArnSucceeds() {
        LambdaInvokerConfig stsConfig = LambdaInvokerConfig.load("lambda-invoker-sts-with-role");
        assertNotNull(stsConfig);
        assertEquals("arn:aws:iam::123456789012:role/TestRole", stsConfig.getRoleArn());
    }
}
