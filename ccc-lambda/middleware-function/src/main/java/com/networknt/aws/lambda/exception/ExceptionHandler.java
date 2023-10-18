package com.networknt.aws.lambda.exception;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.HeaderValue;
import com.networknt.config.Config;
import com.networknt.status.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExceptionHandler {

    private static final String DATA_KEY = "data";
    private static final String NOTIFICATIONS_KEY = "notifications";

    private static final String CONFIG_NAME = "lambda-exception";
    private static final ExceptionConfig CONFIG = (ExceptionConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ExceptionConfig.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static APIGatewayProxyResponseEvent handle(List<Status> middlewareResults) {

        if (CONFIG.isEnabled()) {
            var responseEvent = new APIGatewayProxyResponseEvent();
            var headers = new HashMap<String, String>();

            headers.put(HeaderKey.CONTENT_TYPE, HeaderValue.APPLICATION_JSON);
            responseEvent.setHeaders(headers);

            var returnSchema = new HashMap<String, Object>();
            returnSchema.put(DATA_KEY, null);
            var notifications = new ArrayList<>();

            for (var res : middlewareResults)
                if (res.getCode().startsWith("ERR")) {
                    notifications.add(res);
                    responseEvent.setStatusCode(res.getStatusCode());
                    break;
                }


            returnSchema.put(NOTIFICATIONS_KEY, notifications);

            try {
                var res = OBJECT_MAPPER.writeValueAsString(returnSchema);
                responseEvent.setBody(res);
                return responseEvent;

            } catch (JsonProcessingException e) {
                return new APIGatewayProxyResponseEvent();
            }

        } else return new APIGatewayProxyResponseEvent();


    }

}
