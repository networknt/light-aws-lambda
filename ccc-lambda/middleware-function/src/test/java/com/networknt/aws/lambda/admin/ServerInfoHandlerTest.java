package com.networknt.aws.lambda.admin;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.TestUtils;
import com.networknt.aws.lambda.handler.chain.Chain;
import com.networknt.aws.lambda.handler.info.ServerInfoHandler;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.proxy.LambdaProxy;
import org.junit.jupiter.api.Test;

public class ServerInfoHandlerTest {
    @Test
    public void testServerInfo() {
        APIGatewayProxyRequestEvent requestEvent = TestUtils.createTestRequestEvent();
        requestEvent.setPath("/adm/server/info");
        requestEvent.setHttpMethod("GET");

        Chain requestChain = new Chain(false);
        ServerInfoHandler handler = new ServerInfoHandler();
        requestChain.addChainable(handler);
        requestChain.setupGroupedChain();
        Context lambdaContext = new LambdaContext("1");
        LightLambdaExchange exchange = new LightLambdaExchange(lambdaContext, requestChain);
        exchange.setRequest(requestEvent);

        LambdaProxy lambdaProxy = new LambdaProxy();
        APIGatewayProxyResponseEvent responseEvent = lambdaProxy.handleRequest(requestEvent, lambdaContext);
        System.out.println(responseEvent.toString());
    }
}
