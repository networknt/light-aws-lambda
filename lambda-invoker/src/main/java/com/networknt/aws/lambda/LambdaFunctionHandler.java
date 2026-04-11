package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.config.JsonMapper;
import com.networknt.handler.Handler;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleWithWebIdentityCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LambdaFunctionHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandler.class);
    private static final String MISSING_ENDPOINT_FUNCTION = "ERR10063";
    private static final String EMPTY_LAMBDA_RESPONSE = "ERR10064";
    private static final String STS_TYPE_FUNC_USER = "StsFuncUser";
    private static final String STS_TYPE_WEB_IDENTITY = "StsWebIdentity";
    private static final String BEARER_PREFIX = "BEARER";
    private static final String INVALID_WEB_IDENTITY_TOKEN_MESSAGE = "Missing or invalid Bearer token for STS web identity";
    private static AbstractMetricsHandler metricsHandler;

    private LambdaInvokerConfig config;
    private LambdaAsyncClient client;
    private StsAssumeRoleCredentialsProvider stsCredentialsProvider;
    private MutableStsWebIdentityCredentialsProvider stsWebIdentityCredentialsProvider;
    private StsClient stsClient;

    static final class MutableStsWebIdentityCredentialsProvider implements AwsCredentialsProvider, AutoCloseable {
        private final LambdaInvokerConfig config;
        private final StsClient stsClient;
        private StsAssumeRoleWithWebIdentityCredentialsProvider delegate;
        private String tokenFingerprint;

        MutableStsWebIdentityCredentialsProvider(LambdaInvokerConfig config, StsClient stsClient) {
            this.config = config;
            this.stsClient = stsClient;
        }

        synchronized boolean updateToken(String token) {
            String nextFingerprint = fingerprintToken(token);
            if(nextFingerprint.equals(tokenFingerprint) && delegate != null) {
                return false;
            }
            StsAssumeRoleWithWebIdentityCredentialsProvider nextDelegate =
                    StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
                            .stsClient(stsClient)
                            .refreshRequest(AssumeRoleWithWebIdentityRequest.builder()
                                    .roleArn(config.getRoleArn())
                                    .roleSessionName(config.getRoleSessionName())
                                    .durationSeconds(config.getDurationSeconds())
                                    .webIdentityToken(token)
                                    .build())
                            .build();
            StsAssumeRoleWithWebIdentityCredentialsProvider previousDelegate = delegate;
            delegate = nextDelegate;
            tokenFingerprint = nextFingerprint;
            closeDelegate(previousDelegate);
            return true;
        }

        synchronized String getTokenFingerprint() {
            return tokenFingerprint;
        }

        @Override
        public synchronized AwsCredentials resolveCredentials() {
            if(delegate == null) {
                throw new IllegalStateException("STS web identity credentials provider has not been initialized with a bearer token");
            }
            return delegate.resolveCredentials();
        }

        @Override
        public synchronized void close() {
            closeDelegate(delegate);
            delegate = null;
            tokenFingerprint = null;
        }

        private void closeDelegate(StsAssumeRoleWithWebIdentityCredentialsProvider provider) {
            if(provider != null) {
                try {
                    provider.close();
                } catch (Exception e) {
                    logger.error("Failed to close the StsAssumeRoleWithWebIdentityCredentialsProvider", e);
                }
            }
        }
    }

    // Package-private constructor for testing - avoids loading config from file and metrics chain setup
    LambdaFunctionHandler(LambdaInvokerConfig config) {
        this.config = config;
        this.client = initClient(config);
    }

    public LambdaFunctionHandler() {
        LambdaInvokerConfig config = LambdaInvokerConfig.load();
        this.config = config;
        this.client = initClient(config);
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
        if(logger.isInfoEnabled()) logger.info("LambdaFunctionHandler is loaded.");
    }

    private LambdaAsyncClient initClient(LambdaInvokerConfig config) {
        AwsCredentialsProvider credentialsProvider = null;
        // If any STS type selected, use the matching credentials provider for automatic refresh.
        if(STS_TYPE_FUNC_USER.equals(config.getStsType())) {
            if(logger.isInfoEnabled()) logger.info("STS AssumeRole is set to " + STS_TYPE_FUNC_USER + " for role: {}", config.getRoleArn());
            stsClient = StsClient.builder()
                    .region(Region.of(config.getRegion()))
                    .build();
            stsCredentialsProvider = StsAssumeRoleCredentialsProvider.builder()
                    .stsClient(stsClient)
                    .refreshRequest(AssumeRoleRequest.builder()
                            .roleArn(config.getRoleArn())
                            .roleSessionName(config.getRoleSessionName())
                            .durationSeconds(config.getDurationSeconds())
                            .build())
                    .build();
            credentialsProvider = stsCredentialsProvider;
        } else if(STS_TYPE_WEB_IDENTITY.equals(config.getStsType())) {
            if(logger.isInfoEnabled()) logger.info("STS AssumeRole is set to " + STS_TYPE_WEB_IDENTITY + " for role: {}", config.getRoleArn());
            stsClient = StsClient.builder()
                    .region(Region.of(config.getRegion()))
                    .build();
            stsWebIdentityCredentialsProvider = buildMutableStsWebIdentityCredentialsProvider(config, stsClient);
            credentialsProvider = stsWebIdentityCredentialsProvider;
        } else {
            if(logger.isInfoEnabled()) logger.info("No STS AssumeRole is set. Using default credential provider chain for LambdaAsyncClient.");
        }
        return buildLambdaClient(config, credentialsProvider);
    }

    MutableStsWebIdentityCredentialsProvider buildMutableStsWebIdentityCredentialsProvider(LambdaInvokerConfig config, StsClient stsClient) {
        return new MutableStsWebIdentityCredentialsProvider(config, stsClient);
    }

    LambdaAsyncClient buildLambdaClient(LambdaInvokerConfig config, AwsCredentialsProvider credentialsProvider) {
        SdkAsyncHttpClient asyncHttpClient = NettyNioAsyncHttpClient.builder()
                .readTimeout(Duration.ofMillis(config.getApiCallAttemptTimeout()))
                .writeTimeout(Duration.ofMillis(config.getApiCallAttemptTimeout()))
                .connectionTimeout(Duration.ofMillis(config.getApiCallAttemptTimeout()))
                .maxConcurrency(config.getMaxConcurrency())
                .maxPendingConnectionAcquires(config.getMaxPendingConnectionAcquires())
                .connectionAcquisitionTimeout(Duration.ofSeconds(config.getConnectionAcquisitionTimeout()))
                .build();
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .apiCallTimeout(Duration.ofMillis(config.getApiCallTimeout()))
                .apiCallAttemptTimeout(Duration.ofMillis(config.getApiCallAttemptTimeout()))
                .build();

        if(config.getMaxRetries() > 0) {
            overrideConfig = overrideConfig.toBuilder()
                    .retryStrategy(DefaultRetryStrategy.standardStrategyBuilder()
                            .maxAttempts(config.getMaxRetries() + 1) // +1 because the first attempt is not counted as a retry
                            .build())
                    .build();
        } else {
            overrideConfig = overrideConfig.toBuilder()
                    .retryStrategy(DefaultRetryStrategy.doNotRetry())
                    .build();
        }

        var builder = LambdaAsyncClient.builder().region(Region.of(config.getRegion()))
                .httpClient(asyncHttpClient)
                .overrideConfiguration(overrideConfig);

        if(!StringUtils.isEmpty(config.getEndpointOverride())) {
            builder.endpointOverride(URI.create(config.getEndpointOverride()));
        }

        if(credentialsProvider != null) {
            builder.credentialsProvider(credentialsProvider);
        }

        return builder.build();
    }

    boolean updateWebIdentityToken(String token) {
        if(stsWebIdentityCredentialsProvider == null) {
            throw new IllegalStateException("STS web identity credentials provider is not configured");
        }
        return stsWebIdentityCredentialsProvider.updateToken(token);
    }

    String currentWebIdentityTokenFingerprint() {
        return stsWebIdentityCredentialsProvider == null ? null : stsWebIdentityCredentialsProvider.getTokenFingerprint();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        LambdaInvokerConfig newConfig = LambdaInvokerConfig.load();
        if(newConfig != config) {
            synchronized (this) {
                newConfig = LambdaInvokerConfig.load();
                if(newConfig != config) {
                    config = newConfig;
                    if(client != null) {
                        try {
                            client.close();
                        } catch (Exception e) {
                            logger.error("Failed to close the existing LambdaAsyncClient", e);
                        }
                    }
                    if(stsCredentialsProvider != null) {
                        try {
                            stsCredentialsProvider.close();
                        } catch (Exception e) {
                            logger.error("Failed to close the StsAssumeRoleCredentialsProvider", e);
                        }
                        stsCredentialsProvider = null;
                    }
                    if(stsWebIdentityCredentialsProvider != null) {
                        try {
                            stsWebIdentityCredentialsProvider.close();
                        } catch (Exception e) {
                            logger.error("Failed to close the StsAssumeRoleWithWebIdentityCredentialsProvider", e);
                        }
                        stsWebIdentityCredentialsProvider = null;
                    }
                    if(stsClient != null) {
                        try {
                            stsClient.close();
                        } catch (Exception e) {
                            logger.error("Failed to close the StsClient", e);
                        }
                        stsClient = null;
                    }
                    client = initClient(config);
                    if(config.isMetricsInjection()) {
                        // get the metrics handler from the handler chain for metrics registration. If we cannot get the
                        // metrics handler, then an error message will be logged.
                        Map<String, HttpHandler> handlers = Handler.getHandlers();
                        metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
                        if(metricsHandler == null) {
                            logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
                        }
                    }
                }
            }
        }
        long startTime = System.nanoTime();
        String httpMethod = exchange.getRequestMethod().toString();
        String requestPath = exchange.getRequestPath();
        Map<String, String> headers = convertHeaders(exchange.getRequestHeaders());
        Map<String, String> queryStringParameters = convertQueryParameters(exchange.getQueryParameters());
        Map<String, String> pathParameters = convertPathParameters(exchange.getPathParameters());
        String body = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
        if(logger.isTraceEnabled()) {
            logger.trace("requestPath = {} httpMethod = {} body = {}", requestPath, httpMethod, body);
            logger.trace("headers = {}", JsonMapper.toJson(headers));
            logger.trace("queryParameters = {}", JsonMapper.toJson(queryStringParameters));
            logger.trace("pathParameters = {}", JsonMapper.toJson(pathParameters));
        }
        String endpoint = (String)exchange.getAttachment(AttachmentConstants.AUDIT_INFO).get(Constants.ENDPOINT_STRING);
        String functionName = config.getFunctions().get(endpoint);
        if(functionName == null) {
            setExchangeStatus(exchange, MISSING_ENDPOINT_FUNCTION, endpoint);
            if(config.isMetricsInjection() && metricsHandler != null) metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
            return;
        }
        if(STS_TYPE_WEB_IDENTITY.equals(config.getStsType())) {
            String rawAuthHeader = exchange.getRequestHeaders().getFirst(Headers.AUTHORIZATION);
            String token = extractBearerToken(rawAuthHeader);
            if(token == null || token.isEmpty()) {
                exchange.setStatusCode(401);
                exchange.getResponseSender().send(INVALID_WEB_IDENTITY_TOKEN_MESSAGE);
                if(config.isMetricsInjection() && metricsHandler != null) metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
                return;
            }
            if(updateWebIdentityToken(token)) {
                if(logger.isDebugEnabled()) logger.debug("Authorization token changed. Refreshed the shared STS web identity credentials provider.");
            } else {
                if(logger.isDebugEnabled()) logger.debug("Authorization token unchanged. Reusing the shared STS web identity credentials provider.");
            }
        }
        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setHttpMethod(httpMethod);
        requestEvent.setPath(requestPath);
        requestEvent.setHeaders(headers);
        requestEvent.setPathParameters(pathParameters);
        requestEvent.setQueryStringParameters(queryStringParameters);
        requestEvent.setBody(body);
        String requestBody = JsonMapper.objectMapper.writeValueAsString(requestEvent);
        if(logger.isTraceEnabled()) logger.trace("requestBody = {}", requestBody);
        String res = invokeFunction(client, functionName, requestBody);
        if(logger.isDebugEnabled()) logger.debug("response = {}", res);
        if(res == null) {
            setExchangeStatus(exchange, EMPTY_LAMBDA_RESPONSE, functionName);
            if(config.isMetricsInjection() && metricsHandler != null) metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
            return;
        }
        APIGatewayProxyResponseEvent responseEvent = JsonMapper.fromJson(res, APIGatewayProxyResponseEvent.class);
        setResponseHeaders(exchange, responseEvent.getHeaders());
        exchange.setStatusCode(responseEvent.getStatusCode());
        exchange.getResponseSender().send(responseEvent.getBody());
        if(config.isMetricsInjection() && metricsHandler != null) metricsHandler.injectMetrics(exchange, startTime, config.getMetricsName(), endpoint);
    }

    private String invokeFunction(LambdaAsyncClient client, String functionName, String requestBody)  {
        String response = null;
        try {
            //Need a SdkBytes instance for the payload
            SdkBytes payload = SdkBytes.fromUtf8String(requestBody) ;

            //Setup an InvokeRequest
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .logType(config.getLogType())
                    .payload(payload)
                    .build();
            CompletableFuture<String> futureResponse = client.invoke(request)
                    .thenApply(res -> {
                        if(logger.isTraceEnabled()) logger.trace("LambdaFunctionHandler.invokeFunction response: {}", res);
                        return res.payload().asUtf8String();
                    })
                    .exceptionally(e -> {
                        logger.error("Error invoking lambda function: {}", functionName, e);
                        return null;
                    });
            return futureResponse.get();
        } catch(InterruptedException | ExecutionException e) {
            logger.error("LambdaException", e);
        }
        return null;
    }

    private Map<String, String> convertHeaders(HeaderMap headerMap) {
        Map<String, String> headers = new HashMap<>();
        if(headerMap != null) {
            for(HttpString headerName : headerMap.getHeaderNames()) {
                headers.put(headerName.toString(), headerMap.get(headerName).getFirst());
            }
        }
        return headers;
    }

    private Map<String, String> convertQueryParameters(Map<String, Deque<String>> params) {
        Map<String, String> queryParameters = new HashMap<>();
        if(params != null) {
            for(String key : params.keySet()) {
                queryParameters.put(key, params.get(key).getFirst());
            }
        }
        return queryParameters;

    }

    private Map<String, String> convertPathParameters(Map<String, Deque<String>> params) {
        Map<String, String> pathParameters = new HashMap<>();
        if(params != null) {
            for(String key : params.keySet()) {
                pathParameters.put(key, params.get(key).getFirst());
            }
        }
        return pathParameters;

    }

    private void setResponseHeaders(HttpServerExchange exchange, Map<String, String> headers) {
        if(headers != null) {
            for(String key : headers.keySet()) {
                exchange.getResponseHeaders().put(new HttpString(key), headers.get(key));
            }
        }
    }

    /**
     * Extracts the bearer token from a raw Authorization header value.
     * Returns the token string if the header starts with "Bearer " (case-insensitive),
     * or {@code null} if the header is missing/empty or does not use the Bearer scheme.
     */
    static String extractBearerToken(String authorizationHeaderValue) {
        if (authorizationHeaderValue == null || authorizationHeaderValue.isEmpty()) {
            if(logger.isDebugEnabled()) logger.debug("Missing Authorization header from request. STS AssumeRole with Web Identity may fail");
            return null;
        }
        if (authorizationHeaderValue.length() > BEARER_PREFIX.length() + 1 &&
                authorizationHeaderValue.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length()) &&
                authorizationHeaderValue.charAt(BEARER_PREFIX.length()) == ' ') {
            String token = authorizationHeaderValue.substring(BEARER_PREFIX.length() + 1).trim();
            if (token.isEmpty()) {
                if(logger.isDebugEnabled()) logger.debug("Authorization header contains a blank Bearer token. STS AssumeRole with Web Identity may fail");
                return null;
            }
            return token;
        }
        if(logger.isDebugEnabled()) logger.debug("Authorization header does not start with Bearer. STS AssumeRole with Web Identity may fail");
        return null;
    }

    static String fingerprintToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
