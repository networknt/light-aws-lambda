package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

public class LightRequestHandler {
    public APIGatewayProxyResponseEvent interceptRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // validate, enrich the input and context. If error, return
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);

        return response;
    }

    public APIGatewayProxyResponseEvent interceptResponse(APIGatewayProxyResponseEvent response) {
        return response;
    }
}
