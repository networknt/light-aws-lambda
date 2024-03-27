package com.networknt.aws.lambda.admin;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.TestUtils;
import com.networknt.aws.lambda.handler.Handler;
import com.networknt.aws.lambda.handler.chain.Chain;
import com.networknt.aws.lambda.handler.info.ServerInfoHandler;
import com.networknt.aws.lambda.handler.logger.LoggerGetHandler;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.proxy.LambdaProxy;
import org.junit.jupiter.api.Test;

public class LoggerHandlerTest {
    @Test
    void testGetLogger() {
        APIGatewayProxyRequestEvent requestEvent = TestUtils.createTestRequestEvent();
        requestEvent.setPath("/adm/logger");
        requestEvent.setHttpMethod("GET");
        LambdaProxy lambdaProxy = new LambdaProxy();
        Chain chain = Handler.getChain("/adm/logger@get");
        Context lambdaContext = new LambdaContext("1");
        LightLambdaExchange exchange = new LightLambdaExchange(lambdaContext, chain);
        exchange.setRequest(requestEvent);
        APIGatewayProxyResponseEvent responseEvent = lambdaProxy.handleRequest(requestEvent, lambdaContext);
        System.out.println(responseEvent.toString());

    }

    @Test
    void testSetLogger() {
        APIGatewayProxyRequestEvent requestEvent = TestUtils.createTestRequestEvent();
        requestEvent.setPath("/adm/logger");
        requestEvent.setHttpMethod("POST");
        requestEvent.setBody("[{\"name\":\"ROOT\",\"level\":\"INFO\"},{\"name\":\"com.networknt\",\"level\":\"INFO\"}]");
        LambdaProxy lambdaProxy = new LambdaProxy();
        Chain chain = Handler.getChain("/adm/logger@post");
        Context lambdaContext = new LambdaContext("1");
        LightLambdaExchange exchange = new LightLambdaExchange(lambdaContext, chain);
        exchange.setRequest(requestEvent);
        APIGatewayProxyResponseEvent responseEvent = lambdaProxy.handleRequest(requestEvent, lambdaContext);
        System.out.println(responseEvent.toString());
    }
}
