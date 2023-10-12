package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.exception.ExceptionHandler;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Shared object among middleware threads containing information on the request/response event.
 */
public final class LightLambdaExchange {
    private APIGatewayProxyRequestEvent request;
    private APIGatewayProxyResponseEvent response;
    private final Context context;
    private final Map<Attachable<? extends LambdaMiddleware>, Object> requestAttachments = new HashMap<>();
    private final Map<Attachable<? extends LambdaMiddleware>, Object> responseAttachments = new HashMap<>();
    private final PooledChainLinkExecutor requestExecutor;
    private final PooledChainLinkExecutor responseExecutor;
    private static final int INITIAL_STATE = 0;
    private static final int FLAG_REQUEST_CHAIN_READY = 1 << 1;
    private static final int FLAG_STARTING_REQUEST_READY = 1 << 2;
    private static final int FLAG_REQUEST_DONE = 1 << 3;
    private static final int FLAG_REQUEST_HAS_FAILURE = 1 << 4;
    private static final int FLAG_STARTING_RESPONSE_READY = 1 << 5;
    private static final int FLAG_RESPONSE_CHAIN_READY = 1 << 6;
    private static final int FLAG_RESPONSE_DONE = 1 << 7;
    private static final int FLAG_RESPONSE_HAS_FAILURE = 1 << 8;
    private int state = INITIAL_STATE;
    private int statusCode = 200;

    public LightLambdaExchange(Context context) {
        this.context = context;

        // TODO - add some kind of check to middleware to see if the configured handlers can be used in request and/or response chains.
        this.requestExecutor = new PooledChainLinkExecutor(this, ChainDirection.REQUEST);
        this.responseExecutor = new PooledChainLinkExecutor(this, ChainDirection.RESPONSE);
    }

    public void loadRequestChain(List<String> requestChain) {

        if (stateHasAnyFlags(FLAG_REQUEST_CHAIN_READY))
            return;

        this.loadChain(requestChain, this.requestExecutor);
        this.state |= FLAG_REQUEST_CHAIN_READY;
    }

    public void loadResponseChain(List<String> responseChain) {

        if (stateHasAnyFlags(FLAG_RESPONSE_CHAIN_READY))
            return;

        this.loadChain(responseChain, this.responseExecutor);
        this.state |= FLAG_RESPONSE_CHAIN_READY;
    }

    private void loadChain(List<String> chain, PooledChainLinkExecutor executor) {
        if (chain != null && chain.size() > 0)
            for (var className : chain)
                executor.add(className);
    }

    public void executeRequestChain() {

        if (stateHasAllFlags(FLAG_STARTING_REQUEST_READY | FLAG_REQUEST_CHAIN_READY)) {
            this.requestExecutor.finalizeChain();
            this.requestExecutor.executeChain();
            this.state &= ~FLAG_REQUEST_CHAIN_READY;
        }
    }

    public void executeResponseChain() {

        if (stateHasAllFlags(FLAG_STARTING_RESPONSE_READY | FLAG_RESPONSE_CHAIN_READY)) {
            this.responseExecutor.finalizeChain();
            this.responseExecutor.executeChain();
            this.state &= ~FLAG_RESPONSE_CHAIN_READY;
        }

    }

    public void setResponse(APIGatewayProxyResponseEvent response) {

        if (stateHasAnyFlags(FLAG_STARTING_RESPONSE_READY | FLAG_RESPONSE_DONE | FLAG_RESPONSE_HAS_FAILURE))
            return;

        this.response = response;
        this.statusCode = response.getStatusCode();
        this.state |= FLAG_STARTING_RESPONSE_READY;
    }

    public void setRequest(APIGatewayProxyRequestEvent request) {

        if (stateHasAnyFlags(FLAG_STARTING_REQUEST_READY | FLAG_REQUEST_DONE | FLAG_REQUEST_HAS_FAILURE))
            return;

        this.request = request;
        this.state |= FLAG_STARTING_REQUEST_READY;
    }

    public APIGatewayProxyRequestEvent getRequest() {
        return request;
    }

    public APIGatewayProxyResponseEvent getResponse() {
        if (stateHasAllFlags(FLAG_REQUEST_DONE | FLAG_RESPONSE_DONE)) {

            if (stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE))
                return ExceptionHandler.handle(this.requestExecutor.getChainLinkReturns());

            if (stateHasAnyFlags(FLAG_RESPONSE_HAS_FAILURE))
                return ExceptionHandler.handle(this.responseExecutor.getChainLinkReturns());
        }

        return this.response;
    }

    public Context getContext() {
        return context;
    }

    public <T extends LambdaMiddleware> void addRequestAttachment(Attachable<T> key, Object o) {
        this.requestAttachments.put(key, o);
    }

    public <T extends LambdaMiddleware> void addResponseAttachment(Attachable<T> key, Object o) {
        this.responseAttachments.put(key, o);
    }

    public <T extends LambdaMiddleware> Object getRequestAttachment(Attachable<T> attachable) {
        return this.requestAttachments.get(attachable);
    }

    public <T extends LambdaMiddleware> Object getResponseAttachment(Attachable<T> attachable) {
        return this.responseAttachments.get(attachable);
    }

    public Map<Attachable<? extends LambdaMiddleware>, Object> getRequestAttachments() {
        return requestAttachments;
    }

    public Map<Attachable<? extends LambdaMiddleware>, Object> getResponseAttachments() {
        return responseAttachments;
    }

    private void raiseRequestFailureFlag() {
        if (stateHasAllFlagsClear(FLAG_REQUEST_HAS_FAILURE))
            this.state |= FLAG_REQUEST_HAS_FAILURE;
    }

    public boolean hasFailedState() {
        return stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE | FLAG_RESPONSE_HAS_FAILURE);
    }

    private void raiseResponseFailureFlag() {
        if (stateHasAllFlagsClear(FLAG_RESPONSE_HAS_FAILURE))
            this.state |= FLAG_RESPONSE_HAS_FAILURE;
    }

    public void finalizeRequest() {

        if (stateHasAllFlagsClear(FLAG_REQUEST_DONE)) {

            for (var res : this.requestExecutor.getChainLinkReturns()) {

                // TODO - change this to something more reliable than a string check
                if (res.getCode().startsWith("ERR")) {
                    this.raiseRequestFailureFlag();
                    this.statusCode = res.getStatusCode();
                    break;
                }
            }

            this.state |= FLAG_REQUEST_DONE;
            this.state &= ~FLAG_REQUEST_CHAIN_READY;
            this.state &= ~FLAG_STARTING_REQUEST_READY;
        }

    }

    public void finalizeResponse() {

        if (stateHasAllFlagsClear(FLAG_RESPONSE_DONE)) {

            for (var res : this.responseExecutor.getChainLinkReturns()) {

                // TODO - change this to something more reliable than a string check
                if (res.getCode().startsWith("ERR")) {
                    this.raiseResponseFailureFlag();
                    this.statusCode = res.getStatusCode();
                    break;
                }
            }
            this.state |= FLAG_RESPONSE_DONE;
            this.state &= ~FLAG_RESPONSE_CHAIN_READY;
            this.state &= ~FLAG_STARTING_REQUEST_READY;
        }

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

    private boolean stateHasAnyFlags(int flags) {
        return (this.state & flags) != 0;
    }

    private boolean stateHasAnyFlagsClear(int flags) {
        return (this.state & flags) != flags;
    }

    private boolean stateHasAllFlags(int flags) {
        return (this.state & flags) == flags;
    }

    private boolean stateHasAllFlagsClear(int flags) {
        return (this.state & flags) == 0;
    }

    @Override
    public String toString() {
        return "LightLambdaExchange{" +
                "request=" + request +
                ", response=" + response +
                ", context=" + context +
                ", requestAttachments=" + requestAttachments +
                ", responseAttachments=" + responseAttachments +
                ", requestExecutor=" + requestExecutor +
                ", responseExecutor=" + responseExecutor +
                ", state=" + state +
                ", statusCode=" + statusCode +
                '}';
    }


}