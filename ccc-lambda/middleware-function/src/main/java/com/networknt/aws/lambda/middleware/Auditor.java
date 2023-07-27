package com.networknt.aws.lambda.middleware;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Auditor implements Runnable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(Auditor.class);
    private final LambdaEventWrapper eventWrapper;

    public Auditor(final LambdaEventWrapper eventWrapper) {
        this.eventWrapper = eventWrapper;
    }

    @Override
    public void run() {
        Map<String, Object> auditEntry = new HashMap<>();
        var requestAttachments = eventWrapper.getRequestAttachments();
        var responseAttachments = eventWrapper.getResponseAttachments();

        Map<String, Object> request = new HashMap<>();
        for (var attachment : requestAttachments.entrySet()) {
            if (attachment.getKey().getKey().getAnnotation(ChainProperties.class).audited()) {
                var logKey = attachment.getKey().getKey().getAnnotation(ChainProperties.class).logKey();
                var value = attachment.getValue();
                request.put(logKey, value);
            }
        }
        auditEntry.put("request", request);

        Map<String, Object> response = new HashMap<>();
        for (var attachment : responseAttachments.entrySet()) {
            if (attachment.getKey().getKey().getAnnotation(ChainProperties.class).audited()) {
                var logKey = attachment.getKey().getKey().getAnnotation(ChainProperties.class).logKey();
                var value = attachment.getValue();
                response.put(logKey, value);
            }
        }
        auditEntry.put("response", response);

        try {
            var out = OBJECT_MAPPER.writeValueAsString(auditEntry);
            System.out.println(out);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
