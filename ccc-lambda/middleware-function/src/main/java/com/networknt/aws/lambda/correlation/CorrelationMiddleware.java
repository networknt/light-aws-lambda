package com.networknt.aws.lambda.correlation;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.MiddlewareCallback;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.ByteBuffer;
import java.util.UUID;

public class CorrelationMiddleware extends LambdaMiddleware<String> {

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationMiddleware.class);

    public CorrelationMiddleware(MiddlewareCallback middlewareCallback, APIGatewayProxyRequestEvent input, LambdaContext context) {
        super(middlewareCallback, input, context, true, CorrelationMiddleware.class);
    }

    @Override
    protected MiddlewareReturn<String> executeMiddleware() {

        if (LOG.isDebugEnabled())
            LOG.debug("CorrelationHandler.handleRequest starts.");

        // check if the cid is in the request header
        var cid = this.proxyRequestEvent.getHeaders().get(HeaderKey.TRACEABILITY);

        if (cid == null) {

            cid = this.getUUID();
            this.proxyRequestEvent.getHeaders().put(HeaderKey.CORRELATION, cid);

            var tid = this.proxyRequestEvent.getHeaders().get(HeaderKey.TRACEABILITY);

            if (tid != null && LOG.isInfoEnabled()) {
                LOG.info("Associate traceability Id " + tid + " with correlation Id " + cid);
            }
        }

        MDC.put(LoggerKey.CORRELATION, cid);

        if (LOG.isDebugEnabled())
            LOG.debug("CorrelationHandler.handleRequest ends.");

        return new MiddlewareReturn<>(cid, MiddlewareReturn.Status.EXECUTION_SUCCESS);
    }

    private String getUUID() {
        UUID id = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return Base64.encodeBase64URLSafeString(bb.array());
    }
}
