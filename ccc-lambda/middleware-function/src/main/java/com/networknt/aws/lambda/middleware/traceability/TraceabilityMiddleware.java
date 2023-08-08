package com.networknt.aws.lambda.middleware.traceability;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@ChainProperties()
public class TraceabilityMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddleware.class);

    private static final String CONFIG_NAME = "traceability";
    private static TraceabilityConfig CONFIG = (TraceabilityConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, TraceabilityConfig.class);

    private static final LambdaEventWrapper.Attachable<TraceabilityMiddleware> TRACEABILITY_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(TraceabilityMiddleware.class);

    public TraceabilityMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {

        if (!CONFIG.isEnabled())
            return ChainLinkReturn.disabledMiddlewareReturn();

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware starts.");

        var tid = this.eventWrapper.getRequest().getHeaders().get(HeaderKey.TRACEABILITY);

        if (tid != null) {


            MDC.put(LoggerKey.TRACEABILITY, tid);
            this.eventWrapper.addRequestAttachment(TRACEABILITY_ATTACHMENT_KEY, tid);
        }

        if (LOG.isDebugEnabled())
            LOG.debug("TraceabilityMiddleware.executeMiddleware ends.");

        return ChainLinkReturn.successMiddlewareReturn();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        String configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, TraceabilityConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
