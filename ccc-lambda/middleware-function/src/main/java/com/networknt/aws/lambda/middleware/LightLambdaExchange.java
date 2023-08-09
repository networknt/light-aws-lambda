package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.exception.ExceptionHandler;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
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

    public LightLambdaExchange(Context context, String appId, String env) {
        this.context = context;
        this.requestExecutor = new PooledChainLinkExecutor(this, ChainDirection.REQUEST, appId, env);
        this.responseExecutor = new PooledChainLinkExecutor(this, ChainDirection.RESPONSE, appId, env);
    }

    public void loadRequestChain(List<String> requestChain) {

        if (stateHasAnyFlags(FLAG_REQUEST_CHAIN_READY))
            return;

        if (requestChain != null && requestChain.size() > 0)
            for (var className : requestChain)
                this.requestExecutor.add(className);

        this.state |= FLAG_REQUEST_CHAIN_READY;
    }

    public void loadResponseChain(List<String> responseChain) {

        if (stateHasAnyFlags(FLAG_RESPONSE_CHAIN_READY))
            return;

        if (responseChain != null && responseChain.size() > 0)
            for (var className : responseChain)
                this.responseExecutor.add(className);

        this.state |= FLAG_RESPONSE_CHAIN_READY;
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

    private void raiseResponseFailureFlag() {

        if (stateHasAllFlagsClear(FLAG_RESPONSE_HAS_FAILURE))
            this.state |= FLAG_RESPONSE_HAS_FAILURE;
    }

    public void finalizeRequest() {

        if (stateHasAllFlagsClear(FLAG_REQUEST_DONE)) {

            for (var res : this.requestExecutor.getChainLinkReturns()) {

                if (!res.getStatus().equals(ChainLinkReturn.Status.EXECUTION_SUCCESS)) {
                    this.raiseRequestFailureFlag();
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
                if (!res.getStatus().equals(ChainLinkReturn.Status.EXECUTION_SUCCESS)) {
                    this.raiseResponseFailureFlag();
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

    private boolean stateHasAnyFlagClear(int flags) {
        return (this.state & flags) != flags;
    }

    private boolean stateHasAllFlags(int flags) {
        return (this.state & flags) == flags;
    }

    private boolean stateHasAllFlagsClear(int flags) {
        return (this.state & flags) == 0;
    }
}
