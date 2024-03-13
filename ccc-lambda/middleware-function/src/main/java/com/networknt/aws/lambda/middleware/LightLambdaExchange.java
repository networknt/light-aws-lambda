package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.MiddlewareHandler;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
    private final Map<Attachable<?>, Object> requestAttachments = Collections.synchronizedMap(new HashMap<>());
    private final Map<Attachable<?>, Object> responseAttachments = Collections.synchronizedMap(new HashMap<>());
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

    /**
     *
     * @param context
     * @param requestChain
     * @param responseChain
     */
    public LightLambdaExchange(Context context, Chain requestChain, Chain responseChain) {
        this.context = context;
        this.requestChain = requestChain;
        this.responseChain = responseChain;

        // TODO - add some kind of check to middleware to see if the configured handlers can be used in request and/or response chains.
        this.executor = new PooledChainLinkExecutor();
    }

    /**
     *
     */
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

    /**
     *
     */
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

    /**
     *
     * @param response
     */
    public void setResponse(APIGatewayProxyResponseEvent response) {

        if (stateHasAnyFlags(FLAG_STARTING_RESPONSE_READY | FLAG_RESPONSE_DONE | FLAG_RESPONSE_HAS_FAILURE))
            return;

        this.response = response;
        this.statusCode = response.getStatusCode();
        this.state |= FLAG_STARTING_RESPONSE_READY;
    }

    /**
     *
     * @param request
     */
    public void setRequest(APIGatewayProxyRequestEvent request) {

        if (stateHasAnyFlags(FLAG_STARTING_REQUEST_READY | FLAG_REQUEST_DONE | FLAG_REQUEST_HAS_FAILURE))
            return;

        this.request = request;
        this.state |= FLAG_STARTING_REQUEST_READY;
    }

    /**
     *
     * @return
     */
    public APIGatewayProxyResponseEvent getResponse() {
        if (stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE))
            return ExceptionUtil.convert(this.executor.getChainResults());

        if (stateHasAnyFlags(FLAG_RESPONSE_HAS_FAILURE))
            return ExceptionUtil.convert(this.executor.getChainResults());

        return this.response;
    }

    public Context getContext() {
        return context;
    }
    public APIGatewayProxyRequestEvent getRequest() {
        return request;
    }

    /**
     *
     * @param key
     * @param o
     * @param <T>
     */
    public <T> void addRequestAttachment(Attachable<T> key, Object o) {
        this.requestAttachments.put(key, o);
    }

    /**
     *
     * @param key
     * @param o
     * @param <T>
     */
    public <T> void addResponseAttachment(Attachable<T> key, Object o) {
        this.responseAttachments.put(key, o);
    }

    /**
     *
     * @param attachable
     * @return
     */
    public Object getRequestAttachment(Attachable<?> attachable) {
        return this.requestAttachments.get(attachable);
    }

    /**
     *
     * @param attachable
     * @return
     */
    public Object getResponseAttachment(Attachable<?> attachable) {
        return this.responseAttachments.get(attachable);
    }

    /**
     *
     * @return
     */
    public boolean hasFailedState() {
        LOG.debug("Checking if exchange has a failure state: {}", stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE | FLAG_RESPONSE_HAS_FAILURE));
        return stateHasAnyFlags(FLAG_REQUEST_HAS_FAILURE | FLAG_RESPONSE_HAS_FAILURE);
    }

    /**
     * Checks to see if the exchange is in the 'request in progress' state.
     * The exchange is in the request state when the request chain is ready and has not finished executing.
     *
     * @return - returns true if the exchange is handing the request.
     */
    public boolean isRequestInProgress() {
        return this.stateHasAllFlags(FLAG_STARTING_REQUEST_READY)
                && this.stateHasAllFlagsClear(FLAG_REQUEST_DONE);
    }

    /**
     * Checks to see if the exchange is in the response in progress state.
     * The exchange is in the response state when the request chain is complete, and the response chain is ready and has not finished executing.
     *
     * @return - return true if the exchange is handling the response.
     */
    public boolean isResponseInProgress() {
        return this.stateHasAllFlags(FLAG_REQUEST_DONE | FLAG_STARTING_RESPONSE_READY)
                && this.stateHasAllFlagsClear(FLAG_RESPONSE_DONE);
    }

    /**
     *
     * @param flags
     * @return
     */
    private boolean stateHasAnyFlags(int flags) {
        return (this.state & flags) != 0;
    }

    /**
     *
     * @param flags
     * @return
     */
    private boolean stateHasAnyFlagsClear(int flags) {
        return (this.state & flags) != flags;
    }

    /**
     *
     * @param flags
     * @return
     */
    private boolean stateHasAllFlags(int flags) {
        return (this.state & flags) == flags;
    }

    /**
     *
     * @param flags
     * @return
     */
    private boolean stateHasAllFlagsClear(int flags) {
        return (this.state & flags) == 0;
    }

    /**
     *
     * @return
     */
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

    /**
     *
     * @param <T>
     */
    public static class Attachable<T> {
        private final Class<T> key;

        private Attachable(Class<T> key) {
            this.key = key;
        }

        public Class<T> getKey() {
            return key;
        }

        /**
         * Creates a new attachable key.
         *
         * @param middleware - class to create a key for.
         * @return - returns new attachable instance.
         * @param <T> - given class has to implement the MiddlewareHandler interface.
         */
        public static <T extends MiddlewareHandler> Attachable<T> createMiddlewareAttachable(Class<T> middleware) {
            return new Attachable<>(middleware);
        }
    }


}
