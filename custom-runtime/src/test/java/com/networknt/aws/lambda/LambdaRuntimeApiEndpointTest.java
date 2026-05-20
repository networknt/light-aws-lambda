package com.networknt.aws.lambda;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LambdaRuntimeApiEndpointTest {

    @Test
    void buildsRuntimeApiUrisFromLoopbackEndpoint() {
        URI nextInvocation = LambdaRuntimeApiEndpoint.nextInvocation("127.0.0.1:9001");
        URI response = LambdaRuntimeApiEndpoint.invocationResponse("localhost:9001", "8476a536-e9f4-11e8-9739-2dfe598c3fcd");
        URI error = LambdaRuntimeApiEndpoint.invocationError("169.254.170.2:9001", "request_id.1:2");
        URI ipv6Loopback = LambdaRuntimeApiEndpoint.nextInvocation("[::1]:9001");

        assertEquals("http://127.0.0.1:9001/2018-06-01/runtime/invocation/next", nextInvocation.toString());
        assertEquals("http://localhost:9001/2018-06-01/runtime/invocation/8476a536-e9f4-11e8-9739-2dfe598c3fcd/response", response.toString());
        assertEquals("http://169.254.170.2:9001/2018-06-01/runtime/invocation/request_id.1:2/error", error.toString());
        assertEquals("http://[::1]:9001/2018-06-01/runtime/invocation/next", ipv6Loopback.toString());
    }

    @Test
    void rejectsRuntimeApiEndpointWithExternalHostOrUrlComponents() {
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("example.com:9001"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("127.0.0.1:9001/metadata"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("user@127.0.0.1:9001"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("127.0.0.1"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("127.0.0.1:70000"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("127.999.0.1:9001"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.nextInvocation("0127.0.0.1:9001"));
    }

    @Test
    void rejectsRequestIdThatCannotBeUsedAsSafePathSegment() {
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.validateRequestId(null));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.validateRequestId(""));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.validateRequestId("../metadata"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.validateRequestId("abc%2Fdef"));
        assertThrows(IllegalArgumentException.class, () -> LambdaRuntimeApiEndpoint.validateRequestId("abc\r\ndef"));
    }
}
