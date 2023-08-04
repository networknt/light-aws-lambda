package com.networknt.aws.lambda.exception;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.config.Config;

public class ExceptionHandler {

    private static final String CONFIG_NAME = "exception";

    private static final ExceptionConfig CONFIG = (ExceptionConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ExceptionConfig.class);

    public static void handle(final ChainLinkReturn chainLinkReturn) {
        // TODO - Handle exception
    }

}
