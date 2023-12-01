package com.networknt.aws.lambda.middleware.header;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.header.HeaderConfig;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HeaderMiddleware extends LambdaMiddleware {

    private static final String UNKNOWN_HEADER_OPERATION = "ERR14004";
    private static final String HEADER_MISSING_FOR_OPERATION = "ERR14005";
    private static final HeaderConfig CONFIG = HeaderConfig.load();
    private static final Logger LOG = LoggerFactory.getLogger(HeaderMiddleware.class);

    public HeaderMiddleware() {
        super(false, false, false);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        switch (this.getChainDirection()) {

            case REQUEST:
                return this.handleRequestHeaders(exchange);

            case RESPONSE:
                return this.handleResponseHeaders(exchange);

            default:
                return new Status(UNKNOWN_HEADER_OPERATION);
        }
    }


    private Status handleRequestHeaders(LightLambdaExchange exchange) {
        var headers = exchange.getRequest().getHeaders();
        List<String> removeList = CONFIG.getRequestRemoveList();
        Map<String, Object> updateMap = CONFIG.getRequestUpdateMap();
        if (headers == null || removeList == null || updateMap == null)
            return LambdaMiddleware.successMiddlewareStatus();

        return this.handleTransforms(headers, removeList, updateMap);
    }

    private Status handleResponseHeaders(LightLambdaExchange exchange) {
        var headers = exchange.getResponse().getHeaders();
        List<String> removeList = CONFIG.getResponseRemoveList();
        Map<String, Object> updateMap = CONFIG.getResponseUpdateMap();
        if (headers == null || removeList == null || updateMap == null)
            return LambdaMiddleware.successMiddlewareStatus();

        return this.handleTransforms(headers, removeList, updateMap);
    }

    private Status handleTransforms(Map<String, String> headers, List<String> removeList, Map<String, Object> updateMap) {
        LOG.debug("Using transforms remove '{}' and update '{}' on headers '{}'", removeList, updateMap, headers);
        for (String header : removeList) {
            headers.remove(header);
        }
        for (Map.Entry<String, Object> entry : updateMap.entrySet()) {
            headers.put(entry.getKey(), (String) entry.getValue());
        }
        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getCachedConfigurations() {
    }
}
