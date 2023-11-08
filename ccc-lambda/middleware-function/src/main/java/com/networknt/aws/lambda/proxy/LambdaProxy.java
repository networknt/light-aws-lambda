package com.networknt.aws.lambda.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
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
    public static final LambdaProxyConfig CONFIG = (LambdaProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LambdaProxyConfig.class);
    private static LambdaClient client;
    private static String dynamoDbTableName;
    private static Chain REQUEST_CHAIN;
    private static Chain RESPONSE_CHAIN;

    public LambdaProxy() {
        var builder = LambdaClient.builder().region(Region.of(CONFIG.getRegion()));

        if (!StringUtils.isEmpty(CONFIG.getEndpointOverride()))
            builder.endpointOverride(URI.create(CONFIG.getEndpointOverride()));

        client = builder.build();
        initChains();
    }

    private void initChains() {
        REQUEST_CHAIN = new Chain(false, ChainDirection.REQUEST);
        if (CONFIG.getRequestChain() != null) {
            for (var middleware : CONFIG.getRequestChain()) {
                LOG.debug("Adding new request middleware '{}'", middleware);
                REQUEST_CHAIN.add(middleware);
            }

            REQUEST_CHAIN.setupGroupedChain();
        }


        RESPONSE_CHAIN = new Chain(false, ChainDirection.RESPONSE);
        if (CONFIG.getResponseChain() != null) {
            for (var middleware : CONFIG.getResponseChain()) {
                LOG.debug("Adding new response middleware '{}'", middleware);
                RESPONSE_CHAIN.add(middleware);
            }

            RESPONSE_CHAIN.setupGroupedChain();
        }

    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {
        LOG.debug("Lambda CCC --start");
        final var exchange = new LightLambdaExchange(context, REQUEST_CHAIN, RESPONSE_CHAIN);
        exchange.setRequest(apiGatewayProxyRequestEvent);

        /* exec request chain */
        exchange.executeRequestChain();
        exchange.finalizeRequest();

        if (!exchange.hasFailedState()) {

            LOG.debug("Invoke Time - Start: {}", System.currentTimeMillis());
            /* invoke lambda function */
            var path = exchange.getRequest().getPath();
            var method = exchange.getRequest().getHttpMethod().toLowerCase();
            LOG.debug("Request path: {} -- Request method: {}", path, method);
            var functionName = CONFIG.getFunctions().get(path + "@" + method);
            var res = this.invokeFunction(client, functionName, exchange);
            LOG.debug("Invoke Time - Finish: {}", System.currentTimeMillis());

            var responseEvent = JsonMapper.fromJson(res, APIGatewayProxyResponseEvent.class);
            exchange.setResponse(responseEvent);
            LOG.debug("Exec Response Chain - Start: {}", System.currentTimeMillis());

            /* exec response chain */
            exchange.executeResponseChain();
            LOG.debug("Exec Response Chain - Finish: {}", System.currentTimeMillis());
            exchange.finalizeResponse();



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
}
