package com.networknt.aws.lambda.middleware.traceability;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import com.networknt.status.Status;
import com.networknt.traceability.TraceabilityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TraceabilityMiddleware implements MiddlewareHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddleware.class);
    private static final TraceabilityConfig CONFIG = TraceabilityConfig.load();
    public static final LightLambdaExchange.Attachable<TraceabilityMiddleware> TRACEABILITY_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(TraceabilityMiddleware.class);

    public TraceabilityMiddleware() {
    }

    @Override
    public Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return disabledMiddlewareStatus();

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware starts.");

        var tid = exchange.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

        if (tid != null) {
            MDC.put(LoggerKey.TRACEABILITY, tid);
            exchange.addRequestAttachment(TRACEABILITY_ATTACHMENT_KEY, tid);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware ends.");

        return successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {
        // TODO
    }

    @Override
    public boolean isEnabled() {
        return CONFIG.isEnabled();
    }

    @Override
    public void register() {

    }

    @Override
    public void reload() {

    }

    @Override
    public boolean isContinueOnFailure() {
        return false;
    }

    @Override
    public boolean isAudited() {
        return false;
    }

    @Override
    public boolean isAsynchronous() {
        return false;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        return null;
    }
}
