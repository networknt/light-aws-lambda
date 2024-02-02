package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.Handler;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.metrics.AbstractMetricsHandler;
import com.networknt.metrics.MetricsConfig;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.net.URI;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class LambdaFunctionHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandler.class);
    private static LambdaInvokerConfig config;
    private static final String MISSING_ENDPOINT_FUNCTION = "ERR10063";
    private static final String EMPTY_LAMBDA_RESPONSE = "ERR10064";
    private static AbstractMetricsHandler metricsHandler;

    private static LambdaClient client;

    public LambdaFunctionHandler() {
        config = LambdaInvokerConfig.load();
        LambdaClientBuilder builder = LambdaClient.builder().region(Region.of(config.getRegion()));
        if(!StringUtils.isEmpty(config.getEndpointOverride())) {
            builder.endpointOverride(URI.create(config.getEndpointOverride()));
        }
        client = builder.build();
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
        ModuleRegistry.registerModule(LambdaInvokerConfig.CONFIG_NAME, LambdaFunctionHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(LambdaInvokerConfig.CONFIG_NAME), null);
        if(logger.isInfoEnabled()) logger.info("LambdaFunctionHandler is loaded.");
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        long startTime = System.nanoTime();
        String httpMethod = exchange.getRequestMethod().toString();
        String requestPath = exchange.getRequestPath();
        Map<String, String> headers = convertHeaders(exchange.getRequestHeaders());
        Map<String, String> queryStringParameters = convertQueryParameters(exchange.getQueryParameters());
        Map<String, String> pathParameters = convertPathParameters(exchange.getPathParameters());
        String body = exchange.getAttachment(AttachmentConstants.REQUEST_BODY_STRING);
        if(logger.isTraceEnabled()) {
            logger.trace("requestPath = " + requestPath + " httpMethod = " + httpMethod + " body = " + body);
            logger.trace("headers = " + JsonMapper.toJson(headers));
            logger.trace("queryParameters = " + JsonMapper.toJson(queryStringParameters));
            logger.trace("pathParameters = " + JsonMapper.toJson(pathParameters));
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
        requestEvent.setBody(body == null ? null : body);
        String requestBody = JsonMapper.objectMapper.writeValueAsString(requestEvent);
        if(logger.isTraceEnabled()) logger.trace("requestBody = " + requestBody);
        String res = invokeFunction(client, functionName, requestBody);
        if(logger.isDebugEnabled()) logger.debug("response = " + res);
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

    private String invokeFunction(LambdaClient awsLambda, String functionName, String requestBody)  {
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

            //Invoke the Lambda function
            InvokeResponse res = awsLambda.invoke(request);
            if(logger.isDebugEnabled()) {
                logger.debug("lambda call function error:" + res.functionError());
                logger.debug("lambda logger result:" + res.logResult());
                logger.debug("lambda call status:" + res.statusCode());
            }

            response = res.payload().asUtf8String() ;
        } catch(LambdaException e) {
            logger.error("LambdaException", e);
        }
        return response;
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

    public static void reload() {
        config.reload();
        LambdaClientBuilder builder = LambdaClient.builder().region(Region.of(config.getRegion()));
        if(!StringUtils.isEmpty(config.getEndpointOverride())) {
            builder.endpointOverride(URI.create(config.getEndpointOverride()));
        }
        client = builder.build();
        if(config.isMetricsInjection()) {
            // get the metrics handler from the handler chain for metrics registration. If we cannot get the
            // metrics handler, then an error message will be logged.
            Map<String, HttpHandler> handlers = Handler.getHandlers();
            metricsHandler = (AbstractMetricsHandler) handlers.get(MetricsConfig.CONFIG_NAME);
            if(metricsHandler == null) {
                logger.error("An instance of MetricsHandler is not configured in the handler.yml.");
            }
        }
        ModuleRegistry.registerModule(LambdaInvokerConfig.CONFIG_NAME, LambdaFunctionHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(LambdaInvokerConfig.CONFIG_NAME), null);
        if(logger.isInfoEnabled()) logger.info("LambdaFunctionHandler is loaded.");
    }
}
