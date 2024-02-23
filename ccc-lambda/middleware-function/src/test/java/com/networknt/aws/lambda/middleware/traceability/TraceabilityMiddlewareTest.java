package com.networknt.aws.lambda.middleware.traceability;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.InvocationResponse;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.TestUtils;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.MiddlewareTestBase;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.utility.HeaderKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class TraceabilityMiddlewareTest extends MiddlewareTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddlewareTest.class);

    LightLambdaExchange exchange;

    @Test
    void test() {
        var apiGatewayProxyRequestEvent = TestUtils.createTestRequestEvent();

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
        this.exchange.executeRequestChain();

        // X-Traceability-Id should be added to the exchange as an attachment.
        String traceabilityId = (String) this.exchange.getRequestAttachment(TraceabilityMiddleware.TRACEABILITY_ATTACHMENT_KEY);
        Assertions.assertEquals("123-123-123", traceabilityId);

        var res = this.invokeLocalLambdaFunction(exchange);
        LOG.debug(res);
        Assertions.assertNotNull(res);

    }
}
