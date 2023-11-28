package com.networknt.aws.lambda.middleware.audit;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.security.JwtVerifyMiddleware;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This middleware is responsible for auditing the request and response. it will wire in the end of the response chain and output the
 * information collected in the audit attachment in the exchange to an audit log file. There are several middleware handlers will be
 * responsible to update the attachment in the exchange.
 *
 */
public class AuditMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(JwtVerifyMiddleware.class);
    private static final String CONFIG_NAME = "audit";
    private static final AuditConfig CONFIG = (AuditConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, AuditConfig.class);
    public static final LightLambdaExchange.Attachable<AuditMiddleware> AUDIT_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(AuditMiddleware.class);

    public AuditMiddleware(boolean audited, boolean asynchronous, boolean continueOnFailure) {
        super(false, false, false);
    }

    @Override
    protected Status executeMiddleware(LightLambdaExchange exchange) throws InterruptedException {
        return null;
    }

    @Override
    public void getCachedConfigurations() {

    }
}
