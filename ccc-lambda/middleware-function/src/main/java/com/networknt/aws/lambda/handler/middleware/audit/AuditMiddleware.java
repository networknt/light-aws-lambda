package com.networknt.aws.lambda.handler.middleware.audit;

import com.networknt.audit.AuditConfig;
import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.config.Config;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This middleware is responsible for auditing the request and response. it will wire in the end of the response chain and output the
 * information collected in the audit attachment in the exchange to an audit log file. There are several middleware handlers will be
 * responsible to update the attachment in the exchange.
 *
 */
public class AuditMiddleware implements MiddlewareHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AuditMiddleware.class);
    private static final AuditConfig CONFIG = AuditConfig.load();
    public static final LightLambdaExchange.Attachable<AuditMiddleware> AUDIT_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(AuditMiddleware.class);


    public AuditMiddleware() {
    }

    @Override
    public Status execute(LightLambdaExchange exchange) throws InterruptedException {
        throw new NotImplementedException();
    }

    @Override
    public void getCachedConfigurations() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isEnabled() {
        return CONFIG.isEnabled();
    }

    @Override
    public void register() {
        ModuleRegistry.registerModule(
                AuditConfig.CONFIG_NAME,
                AuditMiddleware.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(AuditConfig.CONFIG_NAME),
                null
        );
    }

    @Override
    public void reload() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isContinueOnFailure() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAudited() {
        throw new NotImplementedException();
    }

    @Override
    public boolean isAsynchronous() {
        return true;
    }


}
