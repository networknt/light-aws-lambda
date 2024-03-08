package com.networknt.aws.lambda.middleware.correlation;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import com.networknt.correlation.CorrelationConfig;
import com.networknt.status.Status;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.ByteBuffer;
import java.util.UUID;


public class CorrelationMiddleware extends LambdaMiddleware implements MiddlewareHandler {

    private static final CorrelationConfig CONFIG = CorrelationConfig.load();
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationMiddleware.class);
    private static final LightLambdaExchange.Attachable<CorrelationMiddleware> CORRELATION_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(CorrelationMiddleware.class);

    public CorrelationMiddleware() {
        super(true, false, false);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {
        LOG.debug("CorrelationHandler.handleRequest starts.");

        // check if the cid is in the request header
        var cid = exchange.getRequest().getHeaders().get(HeaderKey.CORRELATION);

        if (cid == null && CONFIG.isAutogenCorrelationID()) {
            cid = this.getUUID();
            exchange.getRequest().getHeaders().put(HeaderKey.CORRELATION, cid);
            exchange.addRequestAttachment(CORRELATION_ATTACHMENT_KEY, cid);
            var tid = exchange.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

            if (tid != null && LOG.isInfoEnabled())
                LOG.info("Associate traceability Id " + tid + " with correlation Id " + cid);

        }

        if (cid != null)
            MDC.put(LoggerKey.CORRELATION, cid);

        LOG.debug("CorrelationHandler.handleRequest ends.");
        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {
        // TODO
        throw new NotImplementedException();
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

    private String getUUID() {
        UUID id = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return Base64.encodeBase64URLSafeString(bb.array());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        return null;
    }
}
