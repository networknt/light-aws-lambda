package com.networknt.aws.lambda.proxy;

import com.networknt.aws.lambda.middleware.header.HeaderConfig;
import com.networknt.aws.lambda.audit.Audit;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.middleware.body.RequestBodyTransformerMiddleware;
import com.networknt.aws.lambda.middleware.correlation.CorrelationMiddleware;
import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.security.SecurityMiddleware;
import com.networknt.aws.lambda.middleware.traceability.TraceabilityMiddleware;
import com.networknt.config.Config;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;

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

    public LambdaProxy() {
        LambdaClientBuilder builder = LambdaClient.builder().region(Region.of(CONFIG.getRegion()));
        if(!StringUtils.isEmpty(CONFIG.getEndpointOverride())) {
            builder.endpointOverride(URI.create(CONFIG.getEndpointOverride()));
        }
        client = builder.build();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {

        final LambdaEventWrapper eventWrapper = new LambdaEventWrapper();
        eventWrapper.setRequest(apiGatewayProxyRequestEvent);
        eventWrapper.updateContext(context);

        final var auditor = new Audit(eventWrapper);
        final var auditThread = new Thread(auditor);

        // middleware is executed in the order they are added.
        final var requestChain = new PooledChainLinkExecutor(
                eventWrapper,
                ChainDirection.REQUEST,
                "arn:aws:lambda:ca-central-1:442513687360:function:eadp-light-lambda-test",
                "dev")
                .add(SecurityMiddleware.class)
                .add(TraceabilityMiddleware.class)
                .add(CorrelationMiddleware.class)
                .add(RequestBodyTransformerMiddleware.class);

        requestChain.finalizeChain();
        requestChain.executeChain();



        // TODO: Check chain results using 'requestChain.getResolvedChainResults() -- look for failed executions'
        // TODO: if failed, return error in exception handler format
        // TODO: if success, continue to pass request to backend business AWS lambda

        for (var middlewareHandlerStatus : requestChain.getResolvedChainResults()) {
            System.out.println(middlewareHandlerStatus.toString());
        }

        /* send to backend */
//        try {
//
//            InvokeRequest invokeRequest = InvokeRequest.builder()
//                    .functionName("FUNCTION-NAME")
//                    .payload(SdkBytes.fromString("GET FROM REQUEST EXECUTION CHAIN", StandardCharsets.UTF_8))
//                    .build();
//
//            LambdaClient lambdaClient = LambdaClient.builder()
//                    .region(Region.US_EAST_1) // Replace with your desired region
//                    .build();
//
//            InvokeResponse invokeResult = lambdaClient.invoke(invokeRequest);
//
//
//        } catch (ServiceException e) {
//            System.out.println(e);
//        }

        // TODO: check if you get a correct response from backend business API
        // TODO: if failed, return failure back
        // TODO: if success, execute responseChain

        /* update response wrapper with response from business lambda */
//        eventWrapper.setResponse(responseEvent);
//        eventWrapper.updateContext(responseContext);


//        final var responseChain = new PooledChainLinkExecutor(eventWrapper, ChainDirection.RESPONSE)
//                .add(ResponseBodyTransformerMiddleware.class);
//
//        responseChain.finalizeChain();
//        responseChain.executeChain();

        // TODO: Check chain results using 'response.getResolvedChainResults() -- look for failed executions'

        auditThread.start();

        // TODO: if failed, return error in exception handler format
        // TODO: if success, continue to pass response back to client

        try {
            auditThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        /* test response payload for lambda deployment */
        APIGatewayProxyResponseEvent testResponse = new APIGatewayProxyResponseEvent();
        testResponse.setBody(eventWrapper.getRequest().getBody());
        testResponse.setHeaders(eventWrapper.getRequest().getHeaders());



        return testResponse;



//


    }
}
