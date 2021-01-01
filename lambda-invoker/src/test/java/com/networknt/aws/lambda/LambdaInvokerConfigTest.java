package com.networknt.aws.lambda;

import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class LambdaInvokerConfigTest {
    private static LambdaInvokerConfig config = (LambdaInvokerConfig) Config.getInstance().getJsonObjectConfig(LambdaInvokerConfig.CONFIG_NAME, LambdaInvokerConfig.class);

    @Test
    public void testFunctionMapping() {
        Map<String, String> functions = config.getFunctions();
        System.out.println(JsonMapper.toJson(functions));
    }
}
