package com.networknt.aws.lambda.middleware.audit;

import com.networknt.audit.AuditConfig;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.security.JwtVerifyMiddleware;
import com.networknt.status.Status;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This middleware is responsible for auditing the request and response. it will wire in the end of the response chain and output the
 * information collected in the audit attachment in the exchange to an audit log file. There are several middleware handlers will be
 * responsible to update the attachment in the exchange.
 *
 */
public class AuditMiddleware extends LambdaMiddleware {
    private static final Logger LOG = LoggerFactory.getLogger(AuditMiddleware.class);
    private static final AuditConfig CONFIG = AuditConfig.load();
    public static final LightLambdaExchange.Attachable<AuditMiddleware> AUDIT_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(AuditMiddleware.class);

    public AuditMiddleware(boolean audited, boolean asynchronous, boolean continueOnFailure) {
        super(false, false, false);
    }

    @Override
    protected Status executeMiddleware(LightLambdaExchange exchange) throws InterruptedException {
        // TODO
        throw new NotImplementedException();
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
}
