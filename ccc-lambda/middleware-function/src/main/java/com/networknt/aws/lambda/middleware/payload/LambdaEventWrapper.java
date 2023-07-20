package com.networknt.aws.lambda.middleware.payload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;

import java.util.HashMap;
import java.util.Map;

public class LambdaEventWrapper {
    private APIGatewayProxyRequestEvent request;
    private APIGatewayProxyResponseEvent response;
    private Context context;

    private final Map<Class<? extends LambdaMiddleware<?>>, Object> requestAttachments = new HashMap<>();
    private final Map<Class<? extends LambdaMiddleware<?>>, Object> responseAttachments = new HashMap<>();

    private boolean requestSet;
    private boolean responseSet;

    public LambdaEventWrapper() {
        requestSet = false;
        responseSet = false;
    }

    public void updateContext(Context context) {
        this.context = context;
    }

    public void setResponse(APIGatewayProxyResponseEvent response) {

        if (responseSet)
            return;

        this.response = response;
        responseSet = true;
    }

    public void setRequest(APIGatewayProxyRequestEvent request) {

        if (requestSet)
            return;

        this.request = request;
        requestSet = true;
    }

    public APIGatewayProxyRequestEvent getRequest() {
        return request;
    }

    public APIGatewayProxyResponseEvent getResponse() {
        return response;
    }

    public Context getContext() {
        return context;
    }

    public void addRequestAttachment(Class<? extends LambdaMiddleware<?>> middleware, Object o) {
        if (!requestSet)
            return;

        this.requestAttachments.put(middleware, o);
    }

    public void addResponseAttachment(Class<? extends LambdaMiddleware<?>> middleware, Object o) {
        if (!responseSet)
            return;

        this.responseAttachments.put(middleware, o);
    }

    public Object getRequestAttachment(Class<? extends LambdaMiddleware<?>> middleware) {
        if (!requestSet)
            return null;

        return this.requestAttachments.get(middleware);
    }

    public Object getResponseAttachment(Class<? extends LambdaMiddleware<?>> middleware) {
        if (!responseSet)
            return null;

        return this.responseAttachments.get(middleware);
    }
}
