package com.networknt.aws.lambda.middleware.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.HeaderValue;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RequestBodyTransformerMiddleware extends LambdaMiddleware {

    private static final LightLambdaExchange.Attachable<RequestBodyTransformerMiddleware> REQUEST_BODY_ATTACHMENT_KEY = LightLambdaExchange.Attachable.createMiddlewareAttachable(RequestBodyTransformerMiddleware.class);

    private static final Logger LOG = LoggerFactory.getLogger(RequestBodyTransformerMiddleware.class);
    private static final String LAMBDA_BODY_TRANSFORMATION_EXCEPTION = "ERR14002";

    private static final String CONFIG_NAME = "lambda-body";
    private static BodyConfig CONFIG = (BodyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BodyConfig.class);

    public RequestBodyTransformerMiddleware(ChainLinkCallback middlewareCallback, final LightLambdaExchange eventWrapper) {
        super(true, false, false, middlewareCallback, eventWrapper);
    }

    @Override
    protected Status executeMiddleware(final LightLambdaExchange exchange) throws InterruptedException {

        if (!CONFIG.isEnabled())
            return LambdaMiddleware.disabledMiddlewareStatus();

        if (exchange.getRequest().getBody() != null) {
            var body = exchange.getRequest().getBody();

            if (exchange.getRequest().getHeaders().get(HeaderKey.CONTENT_TYPE).equals(HeaderValue.APPLICATION_JSON)) {

                try {
                    var deserializedBody = LambdaMiddleware.OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});

                    // TODO -- DO TRANSFORM HERE

                    var serializedBody = LambdaMiddleware.OBJECT_MAPPER.writeValueAsString(deserializedBody);
                    exchange.getRequest().setBody(serializedBody);
                    exchange.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, serializedBody);
                    return LambdaMiddleware.successMiddlewareStatus();

                } catch (JsonProcessingException e) {
                    LOG.error("Body transformation failed with exception: {}", e.getMessage());
                    exchange.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, body);
                    return new Status(LAMBDA_BODY_TRANSFORMATION_EXCEPTION);
                }

            } else {
                LOG.error("Request body does not have a supported content type.");
                exchange.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, body);
            }
        }

        exchange.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, null);
        return LambdaMiddleware.successMiddlewareStatus();
    }

    @Override
    public void getAppConfigProfileConfigurations(String applicationId, String env) {
        String configResponse = AwsAppConfigUtil.getConfiguration(applicationId, env, CONFIG_NAME);
        if (configResponse != null) {
            try {
                CONFIG = OBJECT_MAPPER.readValue(configResponse, BodyConfig.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
