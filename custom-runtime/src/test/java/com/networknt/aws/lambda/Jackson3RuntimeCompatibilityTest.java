package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.jackson.JacksonDefaults;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Jackson3RuntimeCompatibilityTest {
    @Test
    void runtimeAndFrameworkMappersHaveSeparateUnknownPropertyPolicies() {
        var runtimeMapper = LambdaRuntimeMappers.mapper();
        var frameworkMapper = JacksonDefaults.builder().build();
        assertFalse(runtimeMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertTrue(frameworkMapper.isEnabled(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    void bindsAwsGatewayEventsWithoutJackson2Types() throws Exception {
        var mapper = LambdaRuntimeMappers.mapper();
        var request = mapper.readValue("{\"path\":\"/pets\",\"httpMethod\":\"POST\","
                + "\"body\":\"雪\",\"isBase64Encoded\":true,\"requestContext\":{\"requestId\":\"fixture\"},"
                + "\"multiValueHeaders\":{\"X-Test\":[\"one\",\"two\"]}}", APIGatewayProxyRequestEvent.class);
        assertEquals("/pets", request.getPath());
        assertEquals("fixture", request.getRequestContext().getRequestId());
        assertEquals(List.of("one", "two"), request.getMultiValueHeaders().get("X-Test"));
        assertTrue(request.getIsBase64Encoded());
        assertEquals(request, mapper.readValue(mapper.writeValueAsString(request), APIGatewayProxyRequestEvent.class));
    }

    @Test
    void preservesTheMeasuredResponseWireShape() throws Exception {
        var response = new APIGatewayProxyResponseEvent().withStatusCode(201).withBody("雪\nbody")
                .withIsBase64Encoded(true).withHeaders(Map.of("Content-Type", "application/json"))
                .withMultiValueHeaders(Map.of("Set-Cookie", List.of("a=1", "b=2")));
        assertEquals("{\"statusCode\":201,\"headers\":{\"Content-Type\":\"application/json\"},"
                + "\"multiValueHeaders\":{\"Set-Cookie\":[\"a=1\",\"b=2\"]},\"body\":\"雪\\nbody\","
                + "\"isBase64Encoded\":true}", LambdaRuntimeMappers.mapper().writeValueAsString(response));
    }

    @Test
    void retainsBuilderBindingAndNullPrimitiveHandling() throws Exception {
        var mapper = LambdaRuntimeMappers.mapper();
        var response = mapper.readValue("{\"code\":null,\"message\":\"fixture error\"}", DefaultResponse.class);
        assertEquals(0, response.getCode());
        assertEquals("{\"code\":0,\"message\":\"fixture error\"}", mapper.writeValueAsString(response));
        var invocation = mapper.readValue("{\"requestId\":\"fixture\",\"event\":{\"path\":\"/pets\"}}", InvocationResponse.class);
        assertEquals("fixture", invocation.getRequestId());
        assertEquals("/pets", invocation.getEvent().getPath());
    }

    @Test
    void retainsPermissiveRuntimeBinding() throws Exception {
        var mapper = LambdaRuntimeMappers.mapper();
        var request = mapper.readValue("{\"futureField\":1,\"isBase64Encoded\":false} {}", APIGatewayProxyRequestEvent.class);
        assertNull(request.getPath());
        assertFalse(request.getIsBase64Encoded());
    }
}
