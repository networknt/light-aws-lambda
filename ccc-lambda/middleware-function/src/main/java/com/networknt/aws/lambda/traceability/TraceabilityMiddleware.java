package com.networknt.aws.lambda.traceability;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.MiddlewareCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TraceabilityMiddleware extends LambdaMiddleware<String> {

    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddleware.class);

    public TraceabilityMiddleware(MiddlewareCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, TraceabilityMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<String> executeMiddleware() {

        if (LOG.isDebugEnabled())
            LOG.trace("TraceabilityMiddleware.executeMiddleware starts.");

        var tid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

        if(tid != null) {
            MDC.put(LoggerKey.TRACEABILITY, tid);
        }

        if (LOG.isDebugEnabled())
            LOG.trace("TraceabilityMiddleware.executeMiddleware ends.");

        return new MiddlewareReturn<>(tid, MiddlewareReturn.Status.EXECUTION_SUCCESS);
    }
}
