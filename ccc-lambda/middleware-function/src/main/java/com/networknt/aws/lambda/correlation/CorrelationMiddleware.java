package com.networknt.aws.lambda.correlation;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.ByteBuffer;
import java.util.UUID;

public class CorrelationMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationMiddleware.class);
    private static final LambdaEventWrapper.Attachable CORRELATION_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(CorrelationMiddleware.class);

    public CorrelationMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, true, CorrelationMiddleware.class);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() {

        if (LOG.isDebugEnabled())
            LOG.debug("CorrelationHandler.handleRequest starts.");

        // check if the cid is in the request header
        var cid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

        if (cid == null) {

            cid = this.getUUID();
            this.eventWrapper.getRequest().getHeaders().put(HeaderKey.CORRELATION, cid);
            this.eventWrapper.addRequestAttachment(CORRELATION_ATTACHMENT_KEY, cid);
            var tid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

            if (tid != null && LOG.isInfoEnabled()) {
                LOG.info("Associate traceability Id " + tid + " with correlation Id " + cid);
            }
        }

        MDC.put(LoggerKey.CORRELATION, cid);

        if (LOG.isDebugEnabled())
            LOG.debug("CorrelationHandler.handleRequest ends.");

        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_SUCCESS);
    }

    private String getUUID() {
        UUID id = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return Base64.encodeBase64URLSafeString(bb.array());
    }
}
