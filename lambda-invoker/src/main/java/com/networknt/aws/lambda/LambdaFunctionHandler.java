package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.body.BodyHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.handler.LightHttpHandler;
import com.networknt.httpstring.AttachmentConstants;
import com.networknt.utility.Constants;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class LambdaFunctionHandler implements LightHttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LambdaFunctionHandler.class);
    private static final LambdaInvokerConfig config = (LambdaInvokerConfig) Config.getInstance().getJsonObjectConfig(LambdaInvokerConfig.CONFIG_NAME, LambdaInvokerConfig.class);
    private static final String MISSING_ENDPOINT_FUNCTION = "ERR10063";
    private static final String EMPTY_LAMBDA_RESPONSE = "ERR10064";

    private final LambdaClient client;

    public LambdaFunctionHandler() {
        client = LambdaClient.builder()
                .region(Region.of(config.getRegion()))
                .build();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        String httpMethod = exchange.getRequestMethod().toString();
        String requestPath = exchange.getRequestPath();
        Map<String, String> headers = convertHeaders(exchange.getRequestHeaders());
        Map<String, String> queryStringParameters = convertQueryParameters(exchange.getQueryParameters());
        Map<String, String> pathParameters = convertPathParameters(exchange.getPathParameters());
        String body = exchange.getAttachment(BodyHandler.REQUEST_BODY_STRING);
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
        if(res == null) {
            setExchangeStatus(exchange, EMPTY_LAMBDA_RESPONSE, functionName);
            return;
        }
        APIGatewayProxyResponseEvent responseEvent = JsonMapper.fromJson(res, APIGatewayProxyResponseEvent.class);
        setResponseHeaders(exchange, responseEvent.getHeaders());
        exchange.setStatusCode(responseEvent.getStatusCode());
        exchange.getResponseSender().send(responseEvent.getBody());
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
}
