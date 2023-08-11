package com.networknt.aws.lambda.middleware.header;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class HeaderMiddleware extends LambdaMiddleware {

    public static final String CONFIG_NAME = "lambda-header";
    private static final String UNKNOWN_HEADER_OPERATION = "ERR14004";
    private static final String HEADER_MISSING_FOR_OPERATION = "ERR14005";
    private static HeaderConfig CONFIG = (HeaderConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HeaderConfig.class);
    private static final Logger LOG = LoggerFactory.getLogger(HeaderMiddleware.class);

    public HeaderMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange eventWrapper) {
        super(false, false, false, middlewareCallback, eventWrapper);
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

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        var configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, HeaderConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private Status handleRequestHeaders(LightLambdaExchange exchange) {

        var headers = exchange.getRequest().getHeaders();
        var transforms = CONFIG.getRequestHeader();

        if (headers == null || transforms == null)
            return LambdaMiddleware.successMiddlewareStatus();

        return this.handleTransforms(headers, transforms);
    }

    private Status handleResponseHeaders(LightLambdaExchange exchange) {
        var headers = exchange.getResponse().getHeaders();
        var transforms = CONFIG.getResponseHeader();

        if (headers == null || transforms == null)
            return LambdaMiddleware.successMiddlewareStatus();

        return this.handleTransforms(headers, transforms);
    }

    private Status handleTransforms(Map<String, String> headers, List<HeaderConfig.HeaderChange> headerChanges) {

        LOG.debug("Using transforms '{}' on headers '{}'", headerChanges, headers);

        for (var headerChange : headerChanges) {

            switch (headerChange.getChangeDescriptor().getChangeType()) {

                case REPLACE: {
                    if (headers.get(headerChange.getHeaderKey()) == null)
                        return new Status(HEADER_MISSING_FOR_OPERATION);
                }

                case ADD: {
                    headers.put(headerChange.getHeaderKey(), headerChange.getChangeDescriptor().getValue());
                    break;
                }

                case REMOVE: {
                    headers.remove(headerChange.getHeaderKey());
                    break;
                }

                case APPEND: {
                    var appendedHeader = headers.get(headerChange.getHeaderKey());
                    if (appendedHeader == null)
                        return new Status(HEADER_MISSING_FOR_OPERATION);
                    appendedHeader = appendedHeader + headerChange.getChangeDescriptor().getValue();
                    headers.put(headerChange.getHeaderKey(), appendedHeader);
                    break;
                }

                case PREPEND: {
                    var prependedHeader = headers.get(headerChange.getHeaderKey());
                    if (prependedHeader == null)
                        return new Status(HEADER_MISSING_FOR_OPERATION);
                    prependedHeader = headerChange.getChangeDescriptor().getValue() + prependedHeader;
                    headers.put(headerChange.getHeaderKey(), prependedHeader);
                    break;
                }

                default:
                    return new Status(UNKNOWN_HEADER_OPERATION);
            }
        }

        return LambdaMiddleware.successMiddlewareStatus();
    }
}
