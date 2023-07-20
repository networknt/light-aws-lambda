package com.networknt.aws.lambda;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.networknt.aws.lambda.body.ResponseBodyTransformerMiddleware;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.*;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.body.RequestBodyTransformerMiddleware;
import com.networknt.aws.lambda.correlation.CorrelationMiddleware;
import com.networknt.aws.lambda.header.HeaderMiddleware;
import com.networknt.aws.lambda.limit.LimitMiddleware;
import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.security.SecurityMiddleware;
import com.networknt.aws.lambda.traceability.TraceabilityMiddleware;

import java.nio.charset.StandardCharsets;


/**
 * This is the entry point for the middleware Lambda function that is responsible for cross-cutting concerns for the business Lambda
 * function which is called from the is Lambda function once all cross-cutting concerns are addressed. The middleware Lambda function
 * receives the APIGatewayProxyRequestEvent from the API Gateway and returns the APIGatewayProxyResponseEvent to the API Gateway.
 *
 * @author Steve Hu
 */
public class LambdaProxy implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {

        final LambdaEventWrapper eventWrapper = new LambdaEventWrapper();
        eventWrapper.setRequest(apiGatewayProxyRequestEvent);
        eventWrapper.updateContext(context);

        // middleware is executed in the order they are added.
        final var requestChain = new PooledChainLinkExecutor(eventWrapper)
                .add(SecurityMiddleware.class)
                .add(LimitMiddleware.class)
                .add(CorrelationMiddleware.class)
                .add(TraceabilityMiddleware.class)
                .add(HeaderMiddleware.class)
                .add(RequestBodyTransformerMiddleware.class);

        requestChain.finalizeChain();
        requestChain.executeChain();

        try {

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName("FUNCTION-NAME")
                    .payload(SdkBytes.fromString("GET FROM REQUEST EXECUTION CHAIN", StandardCharsets.UTF_8))
                    .build();

            LambdaClient lambdaClient = LambdaClient.builder()
                    .region(Region.US_EAST_1) // Replace with your desired region
                    .build();

            InvokeResponse invokeResult = lambdaClient.invoke(invokeRequest);


        } catch (ServiceException e) {
            System.out.println(e);
        }

        // TODO get response id?
        final APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        final LambdaContext responseContext = new LambdaContext("" /* id here*/);

        eventWrapper.setResponse(responseEvent);
        eventWrapper.updateContext(responseContext);

        final var responseChain = new PooledChainLinkExecutor(eventWrapper)
                .add(ResponseBodyTransformerMiddleware.class);

        responseChain.finalizeChain();
        responseChain.executeChain();

        return new APIGatewayProxyResponseEvent();

    }
}
