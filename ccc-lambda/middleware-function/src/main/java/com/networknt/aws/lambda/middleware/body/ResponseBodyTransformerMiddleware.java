package com.networknt.aws.lambda.middleware.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.HeaderValue;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ResponseBodyTransformerMiddleware extends LambdaMiddleware {
    Logger LOG = LoggerFactory.getLogger(ResponseBodyTransformerMiddleware.class);
    private static final String CONFIG_NAME = "lambda-body";
    private static final String LAMBDA_BODY_TRANSFORMATION_EXCEPTION = "ERR14002";
    private static BodyConfig CONFIG = (BodyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BodyConfig.class);
    private static final LightLambdaExchange.Attachable<ResponseBodyTransformerMiddleware> RESPONSE_BODY_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(ResponseBodyTransformerMiddleware.class);

    public ResponseBodyTransformerMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange eventWrapper) {
        super(true, false, false, middlewareCallback, eventWrapper);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        if (exchange.getResponse().getBody() != null) {
            var body = exchange.getResponse().getBody();

            if (exchange.getResponse().getHeaders().get(HeaderKey.CONTENT_TYPE).equals(HeaderValue.APPLICATION_JSON)) {
                var deserializedBody = LambdaMiddleware.OBJECT_MAPPER.convertValue(body, new TypeReference<Map<String, Object>>() {});

                // TODO -- DO TRANSFORM HERE

                try {
                    var serializedBody = LambdaMiddleware.OBJECT_MAPPER.writeValueAsString(deserializedBody);
                    exchange.getResponse().setBody(serializedBody);
                    exchange.addResponseAttachment(RESPONSE_BODY_ATTACHMENT_KEY, serializedBody);
                    return LambdaMiddleware.successMiddlewareStatus();

                } catch (JsonProcessingException e) {
                    LOG.error("Body transformation failed with exception: {}", e.getMessage());
                    exchange.addResponseAttachment(RESPONSE_BODY_ATTACHMENT_KEY, body);
                    return new Status(LAMBDA_BODY_TRANSFORMATION_EXCEPTION);
                }

            } else {
                LOG.error("Response body does not have a supported content type.");
                exchange.addResponseAttachment(RESPONSE_BODY_ATTACHMENT_KEY, body);
            }
        }

        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
    }
}
