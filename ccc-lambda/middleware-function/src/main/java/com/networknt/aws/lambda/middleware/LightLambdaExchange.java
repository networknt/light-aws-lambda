package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.middleware.exception.ExceptionMiddleware;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * Shared object among middleware threads containing information on the request/response event.
 */
public final class LightLambdaExchange {

    private static final Logger LOG = LoggerFactory.getLogger(LightLambdaExchange.class);

    // TODO change these request, response members to a more generic type to handle other more edge cases (i.e. lambda stream request/response)
    private APIGatewayProxyRequestEvent request;
    private APIGatewayProxyResponseEvent response;
    private final Context context;
    private final Map<Attachable<? extends LambdaMiddleware>, Object> requestAttachments = new HashMap<>();
    private final Map<Attachable<? extends LambdaMiddleware>, Object> responseAttachments = new HashMap<>();
    private final PooledChainLinkExecutor executor;

    // Initial state
    private static final int INITIAL_STATE = 0;

    // Request chain ready for execution
    private static final int FLAG_STARTING_REQUEST_READY = 1 << 1;

    // Request chain execution complete
    private static final int FLAG_REQUEST_DONE = 1 << 2;

    // Request chain execution complete but had an exception occur
    private static final int FLAG_REQUEST_HAS_FAILURE = 1 << 3;

    // Received response from backend lambda and we are preparing to execute the response chain
    private static final int FLAG_STARTING_RESPONSE_READY = 1 << 4;

    // Response chain execution complete
    private static final int FLAG_RESPONSE_DONE = 1 << 5;

    // Response chain execution complete but had an exception occur
    private static final int FLAG_RESPONSE_HAS_FAILURE = 1 << 6;
    private int state = INITIAL_STATE;
    private int statusCode = 200;
    private final Chain requestChain;
    private final Chain responseChain;

    public LightLambdaExchange(Context context, Chain requestChain, Chain responseChain) {
        this.context = context;
        this.requestChain = requestChain;
        this.responseChain = responseChain;

        // TODO - add some kind of check to middleware to see if the configured handlers can be used in request and/or response chains.
        this.executor = new PooledChainLinkExecutor();
    }

    public void executeRequestChain() {

        if (stateHasAllFlags(FLAG_STARTING_REQUEST_READY)) {

            LOG.debug("Executing request chain");
            this.executor.executeChain(this, this.requestChain);
            this.state &= ~FLAG_STARTING_REQUEST_READY;

            if (stateHasAllFlagsClear(FLAG_REQUEST_DONE)) {
                for (var res : this.executor.getChainResults()) {

                    // TODO - change this to something more reliable than a string check
                    if (res.getCode().startsWith("ERR")) {

                        LOG.debug("Request has failure...");
                        LOG.debug("Setting status code to: {}", res.getStatusCode());

                        this.state |= FLAG_REQUEST_HAS_FAILURE;
                        this.statusCode = res.getStatusCode();
                        break;
                    }
                }
            }

            this.state |= FLAG_REQUEST_DONE;

        }
    }

    public void executeResponseChain() {

        if (stateHasAllFlags(FLAG_STARTING_RESPONSE_READY)) {
            LOG.debug("Executing response chain");
            this.executor.executeChain(this, this.responseChain);
            this.state &= ~FLAG_STARTING_RESPONSE_READY;

            if (stateHasAllFlagsClear(FLAG_RESPONSE_DONE)) {

                for (var res : this.executor.getChainResults()) {

                    // TODO - change this to something more reliable than a string check
                    if (res.getCode().startsWith("ERR")) {
                        this.state |= FLAG_RESPONSE_HAS_FAILURE;
                        this.statusCode = res.getStatusCode();
                        break;
                    }
                }
                this.state |= FLAG_RESPONSE_DONE;
            }

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
        if (stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE))
            return ExceptionMiddleware.handle(this.executor.getChainResults());

        if (stateHasAnyFlags(FLAG_RESPONSE_HAS_FAILURE))
            return ExceptionMiddleware.handle(this.executor.getChainResults());

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

    public boolean hasFailedState() {
        LOG.debug("Checking if exchange has a failure state: {}", stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE | FLAG_RESPONSE_HAS_FAILURE));
        return stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE | FLAG_RESPONSE_HAS_FAILURE);
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

    public String getExchangeStateAsString() {
        String eol = "\",\n";
        return "state {\n" +
                "    \"FLAG_STARTING_REQUEST_READY\": \"" + this.stateHasAnyFlags(FLAG_STARTING_REQUEST_READY) + eol +
                "    \"FLAG_REQUEST_HAS_FAILURE\": " + this.stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE) + eol +
                "    \"FLAG_STARTING_RESPONSE_READY\": \"" + this.stateHasAnyFlags(FLAG_STARTING_RESPONSE_READY) + eol +
                "    \"FLAG_RESPONSE_DONE\": \"" + this.stateHasAnyFlags(FLAG_RESPONSE_DONE) + eol +
                "    \"FLAG_RESPONSE_HAS_FAILURE\": \"" + this.stateHasAnyFlags(FLAG_RESPONSE_HAS_FAILURE) + eol +
                "}";
    }

    @Override
    public String toString() {
        return "LightLambdaExchange{" +
                "request=" + request +
                ", response=" + response +
                ", context=" + context +
                ", requestAttachments=" + requestAttachments +
                ", responseAttachments=" + responseAttachments +
                ", executor=" + executor +
                ", requestChain=" + requestChain +
                ", responseChain=" + responseChain +
                ", state=" + state +
                ", statusCode=" + statusCode +
                '}';
    }


}
