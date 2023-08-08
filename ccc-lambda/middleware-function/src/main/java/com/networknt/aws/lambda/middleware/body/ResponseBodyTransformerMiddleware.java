package com.networknt.aws.lambda.middleware.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.chain.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.utility.AwsAppConfigUtil;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.aws.lambda.utility.HeaderValue;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ChainProperties()
public class ResponseBodyTransformerMiddleware extends LambdaMiddleware {

    Logger LOG = LoggerFactory.getLogger(ResponseBodyTransformerMiddleware.class);
    private static final String CONFIG_NAME = "body";
    private static final String LAMBDA_BODY_TRANSFORMATION_EXCEPTION = "ERR14002";
    private static BodyConfig CONFIG = (BodyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BodyConfig.class);

    private static final LambdaEventWrapper.Attachable<ResponseBodyTransformerMiddleware> RESPONSE_BODY_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(ResponseBodyTransformerMiddleware.class);

    public ResponseBodyTransformerMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() {

        if (!CONFIG.isEnabled())
            return ChainLinkReturn.disabledMiddlewareReturn();

        if (this.eventWrapper.getResponse().getBody() != null) {

            var body = this.eventWrapper.getResponse().getBody();

            if (this.eventWrapper.getResponse().getHeaders().get(HeaderKey.CONTENT_TYPE).equals(HeaderValue.APPLICATION_JSON)) {

                var deserializedBody = LambdaMiddleware.OBJECT_MAPPER.convertValue(body, new TypeReference<Map<String, Object>>() {});

                // TODO -- DO TRANSFORM HERE

                try {

                    var serializedBody = LambdaMiddleware.OBJECT_MAPPER.writeValueAsString(deserializedBody);
                    this.eventWrapper.getResponse().setBody(serializedBody);

                    this.eventWrapper.addResponseAttachment(RESPONSE_BODY_ATTACHMENT_KEY, serializedBody);
                    return ChainLinkReturn.successMiddlewareReturn();

                } catch (JsonProcessingException e) {

                    LOG.error("Body transformation failed with exception: {}", e.getMessage());
                    this.eventWrapper.addResponseAttachment(RESPONSE_BODY_ATTACHMENT_KEY, body);
                    return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, LAMBDA_BODY_TRANSFORMATION_EXCEPTION);
                }

            } else {
                LOG.error("Response body does not have a supported content type.");
                this.eventWrapper.addResponseAttachment(RESPONSE_BODY_ATTACHMENT_KEY, body);
            }
        }

        return ChainLinkReturn.successMiddlewareReturn();
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
