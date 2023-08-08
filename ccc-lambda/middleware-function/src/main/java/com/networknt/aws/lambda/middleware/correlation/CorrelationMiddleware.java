package com.networknt.aws.lambda.middleware.correlation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.LoggerKey;
import com.networknt.config.Config;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.ByteBuffer;
import java.util.UUID;


@ChainProperties()
public class CorrelationMiddleware extends LambdaMiddleware {

    private static final String CONFIG_NAME = "correlation";
    private static CorrelationConfig CONFIG = (CorrelationConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, CorrelationConfig.class);

    private static final Logger LOG = LoggerFactory.getLogger(CorrelationMiddleware.class);

    private static final LambdaEventWrapper.Attachable<CorrelationMiddleware> CORRELATION_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(CorrelationMiddleware.class);

    public CorrelationMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {

        if (!CONFIG.isEnabled())
            return ChainLinkReturn.disabledMiddlewareReturn();

        if (LOG.isDebugEnabled())
            LOG.debug("CorrelationHandler.handleRequest starts.");

        // check if the cid is in the request header
        var cid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.CORRELATION);

        if (cid == null && CONFIG.isAutogenCorrelationID()) {

            cid = this.getUUID();
            this.eventWrapper.getRequest().getHeaders().put(HeaderKey.CORRELATION, cid);
            this.eventWrapper.addRequestAttachment(CORRELATION_ATTACHMENT_KEY, cid);

            var tid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

            if (tid != null && LOG.isInfoEnabled())
                LOG.info("Associate traceability Id " + tid + " with correlation Id " + cid);

        }

        if (cid != null)
            MDC.put(LoggerKey.CORRELATION, cid);

        if (LOG.isDebugEnabled())
            LOG.debug("CorrelationHandler.handleRequest ends.");

        return ChainLinkReturn.successMiddlewareReturn();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        String configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, CorrelationConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getUUID() {
        UUID id = UUID.randomUUID();
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return Base64.encodeBase64URLSafeString(bb.array());
    }
}
