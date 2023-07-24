package com.networknt.aws.lambda.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
import com.networknt.aws.lambda.utility.HeaderKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RequestBodyTransformerMiddleware extends LambdaMiddleware {

    private static final Logger LOG = LoggerFactory.getLogger(RequestBodyTransformerMiddleware.class);
    private static final LambdaEventWrapper.Attachable REQUEST_BODY_ATTACHMENT_KEY = LambdaEventWrapper.Attachable.createMiddlewareAttachable(RequestBodyTransformerMiddleware.class);

    public RequestBodyTransformerMiddleware(ChainLinkCallback middlewareCallback, final LambdaEventWrapper eventWrapper) {
        super(middlewareCallback, eventWrapper, true, true, RequestBodyTransformerMiddleware.class);
    }

    @Override
    protected ChainLinkReturn executeMiddleware() throws InterruptedException {

        if (this.eventWrapper.getRequest().getBody() != null) {

            var body = this.eventWrapper.getRequest().getBody();

            if (this.eventWrapper.getRequest().getHeaders().get(HeaderKey.CONTENT_TYPE).equals("application/json")) {

                try {
                    var deserializedBody = LambdaMiddleware.OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {});

                    // TODO -- DO TRANSFORM HERE

                    /* ----------------------< TEST -- transform >---------------------- */
                    deserializedBody.put("AddedKey", "AddedValue");
                    /*------------------------------------------------------------------*/

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
