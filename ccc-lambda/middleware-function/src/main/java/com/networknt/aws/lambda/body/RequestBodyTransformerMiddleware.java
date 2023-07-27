package com.networknt.aws.lambda.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.chain.ChainLinkReturn;
import com.networknt.aws.lambda.utility.HeaderKey;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ChainProperties(id = "RequestBodyTransformerMiddleware", logKey = "requestBody")
public class RequestBodyTransformerMiddleware extends LambdaMiddleware {

    private static final LambdaEventWrapper.Attachable<RequestBodyTransformerMiddleware> REQUEST_BODY_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(RequestBodyTransformerMiddleware.class);

    private static final Logger LOG = LoggerFactory.getLogger(RequestBodyTransformerMiddleware.class);

    private static final String CONFIG_NAME = "body";
    private static final BodyConfig CONFIG = (BodyConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BodyConfig.class);

    public RequestBodyTransformerMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {

        if (!CONFIG.isEnabled())
            return new ChainLinkReturn(ChainLinkReturn.Status.DISABLED);

        if (this.eventWrapper.getRequest().getBody() != null) {

            var body = this.eventWrapper.getRequest().getBody();

            if (this.eventWrapper.getRequest().getHeaders().get(HeaderKey.CONTENT_TYPE).equals("application/json")) {

                try {
                    var deserializedBody = LambdaMiddleware.OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});

                    // TODO -- DO TRANSFORM HERE

                    var serializedBody = LambdaMiddleware.OBJECT_MAPPER.writeValueAsString(deserializedBody);
                    this.eventWrapper.getRequest().setBody(serializedBody);

                    this.eventWrapper.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, serializedBody);
                    return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_SUCCESS, "Request body transformation was successful.");

                } catch (JsonProcessingException e) {

                    LOG.error("Body transformation failed with exception: {}", e.getMessage());
                    this.eventWrapper.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, body);
                    return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, e.getMessage(), "Failed to re-serialize body after transformation.");
                }

            } else {
                LOG.error("Request body does not have a supported content type.");
                this.eventWrapper.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, body);
            }
        }

        this.eventWrapper.addRequestAttachment(REQUEST_BODY_ATTACHMENT_KEY, null);
        return new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED);
    }
}
