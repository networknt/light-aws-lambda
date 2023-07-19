package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import org.slf4j.MDC;

import java.util.Map;

public class MiddlewareThreadWorker extends Thread {

    public MiddlewareThreadWorker(LambdaMiddleware runnable, AuditThreadContext context) {
        super(runnable);
        MDC.setContextMap(context.MDCContext);
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
        super.run();
    }

    public static class AuditThreadContext {
        final Map<String, String> MDCContext;

        public AuditThreadContext(Map<String, String> context) {
            this.MDCContext = context;
        }
    }
}
