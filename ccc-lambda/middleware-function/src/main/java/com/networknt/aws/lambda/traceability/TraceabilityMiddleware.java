package com.networknt.aws.lambda.traceability;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class TraceabilityMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddleware.class);
    private static final LambdaEventWrapper.Attachable TRACEABILITY_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(TraceabilityMiddleware.class);

    public TraceabilityMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, true, TraceabilityMiddleware.class);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware starts.");

        var tid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

        if(tid != null) {



            MDC.put(LoggerKey.TRACEABILITY, tid);
            this.eventWrapper.addRequestAttachment(TRACEABILITY_ATTACHMENT_KEY, tid);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware ends.");

        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_SUCCESS);
    }
}
