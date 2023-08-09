package com.networknt.aws.lambda.exception;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.config.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExceptionHandler {

    private static final String CONFIG_NAME = "exception";
    private static final String DATA_KEY = "data";
    private static final String NOTIFICATIONS_KEY = "notifications";

    private static final ExceptionConfig CONFIG = (ExceptionConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ExceptionConfig.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static APIGatewayProxyResponseEvent handle(List<ChainLinkReturn> middlewareResults) {
        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent();
        var returnSchema = new HashMap<String, Object>();
        returnSchema.put(DATA_KEY, null);

        var notifications = new ArrayList<>();

        for (var res : middlewareResults) {
            if (!res.getStatus().equals(ChainLinkReturn.Status.EXECUTION_SUCCESS)) {
                notifications.add(res.toStringConditionally(CONFIG.isShowMessage(), CONFIG.isShowDescription(), false));
            }
        }

        returnSchema.put(NOTIFICATIONS_KEY, notifications);

        try {
            var res = OBJECT_MAPPER.writeValueAsString(returnSchema);
            responseEvent.setBody(res);
            return responseEvent;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

}
