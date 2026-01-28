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
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.DefaultRetryStrategy;
import software.amazon.awssdk.services.lambda.LambdaAsyncClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LambdaFunctionHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandler.class);
    private static final String MISSING_ENDPOINT_FUNCTION = "ERR10063";
    private static final String EMPTY_LAMBDA_RESPONSE = "ERR10064";
    private static AbstractMetricsHandler metricsHandler;

    private LambdaInvokerConfig config;
    private LambdaAsyncClient client;

    public LambdaFunctionHandler() {
        LambdaInvokerConfig config = LambdaInvokerConfig.load();
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
        return builder.build();
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
}
