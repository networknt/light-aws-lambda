package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.InvocationResponse;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.middleware.traceability.TraceabilityMiddleware;
import com.networknt.aws.lambda.proxy.LambdaProxy;
import com.networknt.aws.lambda.utility.HeaderKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TraceabilityMiddlewareTest extends MiddlewareTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddlewareTest.class);

    LightLambdaExchange exchange;

    @Test
    void testTraceability() {
        APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = null;
        try {
            apiGatewayProxyRequestEvent = OBJECT_MAPPER.readValue(testEvent, APIGatewayProxyRequestEvent.class);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to read value as APIGatewayProxyRequestEvent");
            throw new RuntimeException(e);
        }
        // add the X-Traceability-Id to the header
        apiGatewayProxyRequestEvent.getHeaders().put(HeaderKey.TRACEABILITY, "123-123-123");
        InvocationResponse invocation = InvocationResponse.builder()
                .requestId("12345")
                .event(apiGatewayProxyRequestEvent)
                .build();
        APIGatewayProxyRequestEvent requestEvent = invocation.getEvent();
        Context lambdaContext = new LambdaContext(invocation.getRequestId());

        Chain requestChain = new Chain(false, ChainDirection.REQUEST);
        TraceabilityMiddleware traceabilityMiddleware = new TraceabilityMiddleware();
        requestChain.addChainable(traceabilityMiddleware);
        requestChain.setupGroupedChain();

        this.exchange = new LightLambdaExchange(lambdaContext, requestChain, null);
        this.exchange.setRequest(requestEvent);
        exchange.executeRequestChain();
        exchange.finalizeRequest();
        // X-Traceability-Id should be added to the exchange as an attachment.
        String traceabilityId = (String) exchange.getRequestAttachment(TraceabilityMiddleware.TRACEABILITY_ATTACHMENT_KEY);
        assert(traceabilityId.equals("123-123-123"));
    }
}
