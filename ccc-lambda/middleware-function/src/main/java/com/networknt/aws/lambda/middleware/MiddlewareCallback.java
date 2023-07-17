package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;

public interface MiddlewareCallback {
    void callback(APIGatewayProxyRequestEvent proxyRequestEvent, Context context, MiddlewareReturn status);
}
