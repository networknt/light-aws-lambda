package com.networknt.aws.lambda.middleware.header;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ChainProperties(audited = false)
public class HeaderMiddleware extends LambdaMiddleware {

    public static final String CONFIG_NAME = "header";
    private static final String UNKNOWN_HEADER_OPERATION = "ERR14004";
    private static final String HEADER_MISSING_FOR_OPERATION = "ERR14005";
    private static HeaderConfig CONFIG = (HeaderConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, HeaderConfig.class);
    private static final Logger LOG = LoggerFactory.getLogger(HeaderMiddleware.class);

    public HeaderMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware(final LightLambdaExchange exchange) {

        if (!CONFIG.isEnabled())
            return ChainLinkReturn.disabledMiddlewareReturn();

        switch (this.getChainDirection()) {

            case REQUEST:
                return this.handleRequestHeaders(exchange);

            case RESPONSE:
                return this.handleResponseHeaders(exchange);

            default:
                return ChainLinkReturn.successMiddlewareReturn();
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

    private ChainLinkReturn handleRequestHeaders(LightLambdaExchange exchange) {

        var headers = exchange.getRequest().getHeaders();
        var transforms = CONFIG.getRequestHeader();

        if (headers == null || transforms == null)
            return ChainLinkReturn.successMiddlewareReturn();

        return this.handleTransforms(headers, transforms);
    }

    private ChainLinkReturn handleResponseHeaders(LightLambdaExchange exchange) {
        var headers = exchange.getResponse().getHeaders();
        var transforms = CONFIG.getResponseHeader();

        if (headers == null || transforms == null)
            return ChainLinkReturn.successMiddlewareReturn();

        return this.handleTransforms(headers, transforms);
    }

    private ChainLinkReturn handleTransforms(Map<String, String> headers, List<HeaderConfig.HeaderChange> headerChanges) {

        LOG.debug("Using transforms '{}' on headers '{}'", headerChanges, headers);

        for (var headerChange : headerChanges) {

            switch (headerChange.getChangeDescriptor().getChangeType()) {

                case REPLACE: {
                    if (headers.get(headerChange.getHeaderKey()) == null)
                        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, HEADER_MISSING_FOR_OPERATION);
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
                        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, HEADER_MISSING_FOR_OPERATION);

                    appendedHeader = appendedHeader + headerChange.getChangeDescriptor().getValue();
                    headers.put(headerChange.getHeaderKey(), appendedHeader);
                    break;
                }

                case PREPEND: {
                    var prependedHeader = headers.get(headerChange.getHeaderKey());

                    if (prependedHeader == null)
                        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, HEADER_MISSING_FOR_OPERATION);

                    prependedHeader = headerChange.getChangeDescriptor().getValue() + prependedHeader;
                    headers.put(headerChange.getHeaderKey(), prependedHeader);
                    break;
                }

                default:
                    return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, UNKNOWN_HEADER_OPERATION);
            }
        }

        return ChainLinkReturn.successMiddlewareReturn();
    }
}
