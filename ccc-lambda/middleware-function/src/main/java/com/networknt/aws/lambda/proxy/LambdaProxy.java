package com.networknt.aws.lambda.proxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.exception.ExceptionHandler;
import com.networknt.aws.lambda.middleware.body.ResponseBodyTransformerMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.audit.Audit;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
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
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

        final var eventWrapper = new LambdaEventWrapper(context);
        eventWrapper.setRequest(apiGatewayProxyRequestEvent);

        try {
            /* exec request chain */
            LambdaProxy.createAndExecuteChain(eventWrapper, CONFIG.getRequestChain(), ChainDirection.REQUEST, CONFIG.getLambdaAppId(), CONFIG.getEnv());

            /* invoke lambda function */
            final var res = this.invokeFunction(client, CONFIG.getFunctions().get("TODO"), eventWrapper);
            final var responseEvent = JsonMapper.fromJson(res, APIGatewayProxyResponseEvent.class);
            eventWrapper.setResponse(responseEvent);

            /* exec response chain */
            LambdaProxy.createAndExecuteChain(eventWrapper, CONFIG.getResponseChain(), ChainDirection.RESPONSE, CONFIG.getLambdaAppId(), CONFIG.getEnv());

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        LOG.debug("Lambda CCC --end");
        return eventWrapper.getResponse();
    }

    private String invokeFunction(LambdaClient awsLambda, String functionName, final LambdaEventWrapper eventWrapper) throws JsonProcessingException {

        String serializedEvent = OBJECT_MAPPER.writeValueAsString(eventWrapper.getRequest());
        String response = null;
        try {
            //Need a SdkBytes instance for the payload
            var payload = SdkBytes.fromUtf8String(serializedEvent);

            //Setup an InvokeRequest
            var request = InvokeRequest.builder()
                    .functionName(functionName)
                    .logType(CONFIG.getLogType())
                    .payload(payload)
                    .build();

            //Invoke the Lambda function
            var res = awsLambda.invoke(request);
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

    private static void createAndExecuteChain(final LambdaEventWrapper eventWrapper, List<String> chainList, ChainDirection direction, String lambdaAppId, String env) {
        final var chain = new PooledChainLinkExecutor(eventWrapper, direction, lambdaAppId, env);

        for (var className : chainList)
            chain.add(className);

        chain.finalizeChain();
        chain.executeChain();
    }
}
