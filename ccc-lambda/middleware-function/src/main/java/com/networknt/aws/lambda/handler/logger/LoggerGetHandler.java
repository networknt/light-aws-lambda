package com.networknt.aws.lambda.handler.logger;

import com.networknt.aws.lambda.handler.LambdaHandler;
import com.networknt.aws.lambda.handler.middleware.LightLambdaExchange;
import com.networknt.status.Status;

public class LoggerGetHandler implements LambdaHandler {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void register() {
//        ModuleRegistry.registerModule(
//                ServerInfoConfig.CONFIG_NAME,
//                ServerInfoMiddleware.class.getName(),
//                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ServerInfoConfig.CONFIG_NAME),
//                null);
    }

    @Override
    public void reload() {

    }

    @Override
    public Status execute(LightLambdaExchange exchange) throws InterruptedException {
        return null;
    }
}
