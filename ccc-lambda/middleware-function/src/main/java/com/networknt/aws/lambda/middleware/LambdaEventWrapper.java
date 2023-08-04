package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared object among middleware threads containing information on the request/response event.
 */
public class LambdaEventWrapper {
    private APIGatewayProxyRequestEvent request;
    private APIGatewayProxyResponseEvent response;
    private Context context;
    private final Map<Attachable<? extends LambdaMiddleware>, Object> requestAttachments = new HashMap<>();
    private final Map<Attachable<? extends LambdaMiddleware>, Object> responseAttachments = new HashMap<>();

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

    public <T extends LambdaMiddleware> void addRequestAttachment(Attachable<T> key, Object o) {
        if (!requestSet)
            return;

        this.requestAttachments.put(key, o);
    }

    public <T extends LambdaMiddleware> void addResponseAttachment(Attachable<T> key, Object o) {
        if (!responseSet)
            return;

        this.responseAttachments.put(key, o);
    }

    public <T extends LambdaMiddleware> Object getRequestAttachment(Attachable<T> attachable) {
        if (!requestSet)
            return null;

        return this.requestAttachments.get(attachable);
    }

    public <T extends LambdaMiddleware> Object getResponseAttachment(Attachable<T> attachable) {
        if (!responseSet)
            return null;

        return this.responseAttachments.get(attachable);
    }

    public Map<Attachable<? extends LambdaMiddleware>, Object> getRequestAttachments() {
        return requestAttachments;
    }

    public Map<Attachable<? extends LambdaMiddleware>, Object> getResponseAttachments() {
        return responseAttachments;
    }

    public static class Attachable<T extends LambdaMiddleware> {
        private final Class<T> key;

        private Attachable(Class<T> key) {
            this.key = key;
        }

        public Class<T> getKey() {
            return key;
        }

        public static <T extends LambdaMiddleware> Attachable<T> createMiddlewareAttachable(Class<T> middleware) {
            return new Attachable<T>(middleware);
        }
    }
}
