package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LambdaFunctionHandlerTest {
    static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandlerTest.class);

    @Test
    public void testAPIGatewayProxyRequestEvent() throws Exception {
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

    // -------------------------------------------------------------------------
    // STS config validation tests
    // -------------------------------------------------------------------------

    @Test
    public void testStsValidationNullRoleArnThrowsException() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setStsEnabled(true);
        config.setRoleArn(null);
        config.setDurationSeconds(3600);
        assertThrows(IllegalArgumentException.class,
                () -> LambdaFunctionHandler.validateStsConfig(config));
    }

    @Test
    public void testStsValidationEmptyRoleArnThrowsException() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setStsEnabled(true);
        config.setRoleArn("");
        config.setDurationSeconds(3600);
        assertThrows(IllegalArgumentException.class,
                () -> LambdaFunctionHandler.validateStsConfig(config));
    }

    @Test
    public void testStsValidationDurationBelowMinimumThrowsException() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setStsEnabled(true);
        config.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        config.setDurationSeconds(899);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LambdaFunctionHandler.validateStsConfig(config));
        assertTrue(ex.getMessage().contains("899"));
    }

    @Test
    public void testStsValidationDurationAboveMaximumThrowsException() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setStsEnabled(true);
        config.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        config.setDurationSeconds(43201);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LambdaFunctionHandler.validateStsConfig(config));
        assertTrue(ex.getMessage().contains("43201"));
    }

    @Test
    public void testStsValidationBoundaryMinimumDurationIsValid() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setStsEnabled(true);
        config.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        config.setDurationSeconds(900);
        assertDoesNotThrow(() -> LambdaFunctionHandler.validateStsConfig(config));
    }

    @Test
    public void testStsValidationBoundaryMaximumDurationIsValid() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setStsEnabled(true);
        config.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        config.setDurationSeconds(43200);
        assertDoesNotThrow(() -> LambdaFunctionHandler.validateStsConfig(config));
    }

    // -------------------------------------------------------------------------
    // STS credentials provider wiring test
    // -------------------------------------------------------------------------

    /**
     * Verify that when STS is enabled and assumeRole returns mock credentials, the handler stores those
     * credentials (to be used via StaticCredentialsProvider when building the LambdaAsyncClient).
     */
    @Test
    public void testStsCredentialsProviderWiredToLambdaClient() throws Exception {
        Instant expiration = Instant.now().plusSeconds(3600);
        Credentials mockCredentials = Credentials.builder()
                .accessKeyId("AKIATEST12345")
                .secretAccessKey("secretKey/testSecret")
                .sessionToken("sessionToken12345")
                .expiration(expiration)
                .build();

        LambdaInvokerConfig config = buildTestStsConfig();

        // Subclass overrides assumeRole so no real STS call is made
        LambdaFunctionHandler handler = new LambdaFunctionHandler(config) {
            @Override
            protected Credentials assumeRole(LambdaInvokerConfig cfg) {
                return mockCredentials;
            }
        };

        try {
            // Verify stsCredentials field was populated with the mock credentials
            Field stsCredentialsField = LambdaFunctionHandler.class.getDeclaredField("stsCredentials");
            stsCredentialsField.setAccessible(true);
            Credentials stored = (Credentials) stsCredentialsField.get(handler);
            assertNotNull(stored, "stsCredentials must be set when STS is enabled");
            assertEquals("AKIATEST12345", stored.accessKeyId());
            assertEquals("secretKey/testSecret", stored.secretAccessKey());
            assertEquals("sessionToken12345", stored.sessionToken());

            // Verify credentialsExpiration was set from the mock credentials
            Field expirationField = LambdaFunctionHandler.class.getDeclaredField("credentialsExpiration");
            expirationField.setAccessible(true);
            Instant storedExpiration = (Instant) expirationField.get(handler);
            assertEquals(expiration, storedExpiration, "credentialsExpiration must match the STS credentials expiry");
        } finally {
            closeHandlerClient(handler);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a fully-populated config suitable for handler instantiation in tests. */
    private LambdaInvokerConfig buildTestStsConfig() {
        LambdaInvokerConfig config = new LambdaInvokerConfig();
        config.setRegion("us-east-1");
        config.setApiCallTimeout(60000);
        config.setApiCallAttemptTimeout(20000);
        config.setMaxConcurrency(50);
        config.setMaxPendingConnectionAcquires(10000);
        config.setConnectionAcquisitionTimeout(10);
        config.setLogType("Tail");
        config.setMaxRetries(0);
        config.setStsEnabled(true);
        config.setRoleArn("arn:aws:iam::123456789012:role/TestRole");
        config.setDurationSeconds(3600);
        config.setRoleSessionName("test-session");
        return config;
    }

    /** Closes the LambdaAsyncClient held by the handler to release Netty thread-pool resources. */
    private void closeHandlerClient(LambdaFunctionHandler handler) {
        try {
            Field clientField = LambdaFunctionHandler.class.getDeclaredField("client");
            clientField.setAccessible(true);
            LambdaAsyncClient lambdaClient = (LambdaAsyncClient) clientField.get(handler);
            if(lambdaClient != null) {
                lambdaClient.close();
            }
        } catch(Exception e) {
            logger.warn("Failed to close LambdaAsyncClient in test cleanup", e);
        }
    }
}
