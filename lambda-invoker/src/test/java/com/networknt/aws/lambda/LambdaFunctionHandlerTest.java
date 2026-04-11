package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LambdaFunctionHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandlerTest.class);

    @Test
    void testAPIGatewayProxyRequestEvent() throws Exception {
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setHttpMethod("get");
        requestEvent.setPath("/pets");
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");
        requestEvent.setHeaders(headers);
        Map<String, String> pathParameters = new HashMap<>();
        requestEvent.setPathParameters(pathParameters);
        Map<String, String> queryStringParameters = new HashMap<>();
        requestEvent.setQueryStringParameters(queryStringParameters);
        requestEvent.setBody(null);
        System.out.println(JsonMapper.objectMapper.writeValueAsString(requestEvent));
    }

    // --- extractBearerToken tests ---

    @Test
    void testExtractBearerToken_nullHeader_returnsNull() {
        assertNull(LambdaFunctionHandler.extractBearerToken(null));
    }

    @Test
    void testExtractBearerToken_emptyHeader_returnsNull() {
        assertNull(LambdaFunctionHandler.extractBearerToken(""));
    }

    @Test
    void testExtractBearerToken_validBearerMixedCase_extractsToken() {
        assertEquals("mytoken123", LambdaFunctionHandler.extractBearerToken("Bearer mytoken123"));
    }

    @Test
    void testExtractBearerToken_validBearerLowercase_extractsToken() {
        assertEquals("mytoken123", LambdaFunctionHandler.extractBearerToken("bearer mytoken123"));
    }

    @Test
    void testExtractBearerToken_validBearerUppercase_extractsToken() {
        assertEquals("mytoken123", LambdaFunctionHandler.extractBearerToken("BEARER mytoken123"));
    }

    @Test
    void testExtractBearerToken_bearerWithJwtToken_extractsFullToken() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.signature";
        assertEquals(jwt, LambdaFunctionHandler.extractBearerToken("Bearer " + jwt));
    }

    @Test
    void testExtractBearerToken_nonBearerScheme_returnsNull() {
        assertNull(LambdaFunctionHandler.extractBearerToken("Basic dXNlcjpwYXNz"));
    }

    @Test
    void testExtractBearerToken_headerIsBearerOnly_returnsNull() {
        // "BEARER" alone is exactly BEARER_PREFIX.length() characters, not greater, so returns null
        assertNull(LambdaFunctionHandler.extractBearerToken("BEARER"));
    }

    @Test
    void testExtractBearerToken_tokenIsEmptyAfterBearer_returnsNull() {
        // Empty bearer tokens are treated as invalid and return null so the STS path skips refresh.
        assertNull(LambdaFunctionHandler.extractBearerToken("Bearer "));
    }
}
