package com.networknt.aws.lambda.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.cache.LambdaCache;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.LambdaEnvVariables;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.net.URI;


/**
 * This is the entry point for the middleware Lambda function that is responsible for cross-cutting concerns for the business Lambda
 * function which is called from the is Lambda function once all cross-cutting concerns are addressed. The middleware Lambda function
 * receives the APIGatewayProxyRequestEvent from the API Gateway and returns the APIGatewayProxyResponseEvent to the API Gateway.
 *
 * @author Steve Hu
 */
public class LambdaProxy implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaProxy.class);
    private static final String CONFIG_NAME = "lambda-proxy";
    private static final LambdaProxyConfig CONFIG = (LambdaProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LambdaProxyConfig.class);
    private static LambdaClient client;
    private static String dynamoDbTableName;

    public LambdaProxy() {
        var builder = LambdaClient.builder().region(Region.of(CONFIG.getRegion()));

        if (!StringUtils.isEmpty(CONFIG.getEndpointOverride()))
            builder.endpointOverride(URI.create(CONFIG.getEndpointOverride()));

        client = builder.build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {
        LOG.debug("Lambda CCC --start");

        // TODO - remove this. This is here just so I can test table creation...
        if (System.getenv(LambdaEnvVariables.CLEAR_AWS_DYNAMO_DB_TABLES).equals("true")) {
            try {
                LambdaCache.getInstance().deleteTable(getLambdaProxyCacheDynamoDbTableName());
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to delete table: " + getLambdaProxyCacheDynamoDbTableName(), e);
            }
        }

        if (CONFIG.isEnableDynamoDbCache() && !LambdaCache.getInstance().doesTableExist(getLambdaProxyCacheDynamoDbTableName())) {
            LOG.debug("Creating new table '{}'", getLambdaProxyCacheDynamoDbTableName());
            try {
                LambdaCache.getInstance().initCacheTable();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }



        final var exchange = new LightLambdaExchange(context);
        exchange.setRequest(apiGatewayProxyRequestEvent);

        LOG.debug("exchange state: {}", exchange);

        /* exec request chain */
        exchange.loadRequestChain(CONFIG.getRequestChain());
        exchange.executeRequestChain();
        exchange.finalizeRequest();

        LOG.debug("exchange state: {}", exchange);

        if (!exchange.hasFailedState()) {

            /* invoke lambda function */
            var path = exchange.getRequest().getPath();
            var method = exchange.getRequest().getHttpMethod().toLowerCase();
            LOG.debug("Request path: {} -- Request method: {}", path, method);

            var functionName = CONFIG.getFunctions().get(path + "@" + method);
            LOG.debug("Found function name: {}", functionName);

            var res = this.invokeFunction(client, functionName, exchange);
            LOG.debug("Response Raw: {}", res);
            var responseEvent = JsonMapper.fromJson(res, APIGatewayProxyResponseEvent.class);

            LOG.debug("Res Body: {}", responseEvent.getBody());
            LOG.debug("Res Headers: {}", responseEvent.getHeaders());

            exchange.setResponse(responseEvent);

            LOG.debug("exchange state: {}", exchange);

            /* exec response chain */
            exchange.loadResponseChain(CONFIG.getResponseChain());
            exchange.executeResponseChain();
            exchange.finalizeResponse();

            LOG.debug("exchange state: {}", exchange);
        }

        LOG.debug("Lambda CCC --end");
        return exchange.getResponse();


    }

    private String invokeFunction(final LambdaClient client, String functionName, final LightLambdaExchange eventWrapper) {

        String serializedEvent = null;
        try {
            serializedEvent = Config.getInstance().getMapper().writeValueAsString(eventWrapper.getRequest());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        String response = null;

        try {
            var payload = SdkBytes.fromUtf8String(serializedEvent);
            var request = InvokeRequest.builder()
                    .functionName(functionName)
                    .logType(CONFIG.getLogType())
                    .payload(payload)
                    .build();
            var res = client.invoke(request);

            if (LOG.isDebugEnabled()) {
                LOG.debug("lambda call function error:" + res.functionError());
                LOG.debug("lambda logger result:" + res.logResult());
                LOG.debug("lambda call status:" + res.statusCode());
            }

            response = res.payload().asUtf8String();
        } catch (LambdaException e) {
            LOG.error("LambdaException", e);
        }
        return response;
    }

    public static String getLambdaProxyCacheDynamoDbTableName() {
        if (dynamoDbTableName == null) {
            dynamoDbTableName = LambdaCache.getDynamoDbTableName(CONFIG.getLambdaAppId());
        }
        return dynamoDbTableName;
    }
}
