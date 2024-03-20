package com.networknt.aws.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.Handler;
import com.networknt.aws.lambda.handler.LambdaFunctionEntry;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.handler.chain.Chain;
import com.networknt.aws.lambda.handler.middleware.validator.ValidatorMiddleware;
import com.networknt.config.Config;
import com.networknt.openapi.ValidatorConfig;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * This is the entry point for the middleware Lambda function that is responsible for cross-cutting concerns for the business Lambda
 * function which is called from the is Lambda function once all cross-cutting concerns are addressed. The middleware Lambda function
 * receives the APIGatewayProxyRequestEvent from the API Gateway and returns the APIGatewayProxyResponseEvent to the API Gateway.
 *
 * @author Steve Hu
 */
public class LambdaProxy implements LambdaFunctionEntry {

    private static final Logger LOG = LoggerFactory.getLogger(LambdaProxy.class);
    private static final String CONFIG_NAME = "lambda-proxy";
    public static final LambdaProxyConfig CONFIG = (LambdaProxyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LambdaProxyConfig.class);

    public LambdaProxy() {
        if (LOG.isInfoEnabled()) LOG.info("LambdaProxy is constructed");
        Handler.init();
        ModuleRegistry.registerModule(
                LambdaProxyConfig.CONFIG_NAME,
                LambdaProxy.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(LambdaProxyConfig.CONFIG_NAME),
                null
        );
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {
        LOG.debug("Lambda CCC --start with request: {}", apiGatewayProxyRequestEvent);
        var requestPath = apiGatewayProxyRequestEvent.getPath();
        var requestMethod = apiGatewayProxyRequestEvent.getHttpMethod();
        LOG.debug("Request path: {} -- Request method: {}", requestPath, requestMethod);
        Chain chain = Handler.getChain(requestPath + "@" + requestMethod);
        if(chain == null) chain = Handler.getDefaultChain();
        final var exchange = new LightLambdaExchange(context, chain);
        exchange.setRequest(apiGatewayProxyRequestEvent);
        exchange.executeChain();
        APIGatewayProxyResponseEvent response = exchange.getResponse();
        LOG.debug("Lambda CCC --end with response: {}", response);
        return response;
    }

}
