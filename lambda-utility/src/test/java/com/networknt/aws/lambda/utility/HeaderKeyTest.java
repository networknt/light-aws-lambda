package com.networknt.aws.lambda.utility;

import org.junit.jupiter.api.Test;

public class HeaderKeyTest {
    @Test
    public void testHeaderKey() {
        assert HeaderKey.TRACEABILITY.equals("X-Traceability-Id");
    }
}
