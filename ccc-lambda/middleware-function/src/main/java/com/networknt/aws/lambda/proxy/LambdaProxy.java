package com.networknt.aws.lambda.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public LambdaProxy() {
        LambdaClientBuilder builder = LambdaClient.builder().region(Region.of(CONFIG.getRegion()));
        if (!StringUtils.isEmpty(CONFIG.getEndpointOverride())) {
            builder.endpointOverride(URI.create(CONFIG.getEndpointOverride()));
        }
        client = builder.build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {
        LOG.debug("Lambda CCC --start");

        final var exchange = new LightLambdaExchange(context, CONFIG.getLambdaAppId(), CONFIG.getEnv());
        exchange.setRequest(apiGatewayProxyRequestEvent);


        /* exec request chain */
        exchange.loadRequestChain(CONFIG.getRequestChain());
        exchange.executeRequestChain();
        exchange.finalizeRequest();

        /* invoke lambda function */
        //final var res = this.invokeFunction(client, CONFIG.getFunctions().get("TODO"), exchange);
        //final var responseEvent = JsonMapper.fromJson(res, APIGatewayProxyResponseEvent.class);

        // TODO - for testing we just reflect the incoming event as the response
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        responseEvent.setBody(exchange.getRequest().getBody());
        responseEvent.setHeaders(exchange.getRequest().getHeaders());
        exchange.setResponse(responseEvent);

        /* exec response chain */
        exchange.loadResponseChain(CONFIG.getResponseChain());
        exchange.executeResponseChain();
        exchange.finalizeResponse();


        LOG.debug("Lambda CCC --end");
        return exchange.getResponse();
    }

    private String invokeFunction(final LambdaClient client, String functionName, final LightLambdaExchange eventWrapper) {

        String serializedEvent = null;
        try {
            serializedEvent = OBJECT_MAPPER.writeValueAsString(eventWrapper.getRequest());
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
