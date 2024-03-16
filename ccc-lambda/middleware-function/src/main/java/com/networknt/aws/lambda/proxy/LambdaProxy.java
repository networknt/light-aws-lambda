package com.networknt.aws.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.Handler;
import com.networknt.aws.lambda.handler.LambdaFunctionEntry;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.handler.chain.Chain;
import com.networknt.config.Config;
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
        Handler.init();
    }


    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final Context context) {
        LOG.debug("Lambda CCC --start with request: {}", apiGatewayProxyRequestEvent);
        var requestPath = apiGatewayProxyRequestEvent.getPath();
        //Chain chain = Handler.getChainForPath(requestPath);
        final var exchange = new LightLambdaExchange(context, new Chain(false));
        exchange.setRequest(apiGatewayProxyRequestEvent);

        APIGatewayProxyResponseEvent response = exchange.getResponse();
        LOG.debug("Lambda CCC --end with response: {}", response);
        return response;
    }



    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

    }
}
