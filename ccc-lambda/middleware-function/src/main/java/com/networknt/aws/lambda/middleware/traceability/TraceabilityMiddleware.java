package com.networknt.aws.lambda.middleware.traceability;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import com.networknt.status.Status;
import com.networknt.traceability.TraceabilityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TraceabilityMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddleware.class);
    private static final String CONFIG_NAME = "lambda-traceability";
    private static final TraceabilityConfig CONFIG = TraceabilityConfig.load();
    public static final LightLambdaExchange.Attachable<TraceabilityMiddleware> TRACEABILITY_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(TraceabilityMiddleware.class);

    public TraceabilityMiddleware() {
        super(true, false, false);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware starts.");

        var tid = exchange.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

        if (tid != null) {
            MDC.put(LoggerKey.TRACEABILITY, tid);
            exchange.addRequestAttachment(TRACEABILITY_ATTACHMENT_KEY, tid);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware ends.");

        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {
    }
}
