package com.networknt.aws.lambda.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.chain.ChainProperties;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Audit implements Runnable {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Logger LOG = LoggerFactory.getLogger(Audit.class);
    private static final String CONFIG_NAME = "audit";
    private static final AuditConfig CONFIG = (AuditConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, AuditConfig.class);
    private final LambdaEventWrapper eventWrapper;

    public Audit(final LambdaEventWrapper eventWrapper) {
        this.eventWrapper = eventWrapper;
    }

    @Override
    public void run() {
//        if (CONFIG.isEnabled()) {
//            var auditEntry = new HashMap<String, Object>();
//            var requestAttachments = eventWrapper.getRequestAttachments();
//            var responseAttachments = eventWrapper.getResponseAttachments();
//
//            var request = new HashMap<String, Object>();
//
//            for (var attachment : requestAttachments.entrySet()) {
//
//                if (attachment.getKey().getKey().getAnnotation(ChainProperties.class).audited()) {
//                    var logKey = attachment.getKey().getKey().getAnnotation(ChainProperties.class).logKey();
//                    var value = attachment.getValue();
//                    request.put(logKey, value);
//                }
//            }
//            auditEntry.put("request", request);
//
//            var response = new HashMap<String, Object>();
//
//            for (var attachment : responseAttachments.entrySet()) {
//
//                if (attachment.getKey().getKey().getAnnotation(ChainProperties.class).audited()) {
//                    var logKey = attachment.getKey().getKey().getAnnotation(ChainProperties.class).logKey();
//                    var value = attachment.getValue();
//                    response.put(logKey, value);
//                }
//            }
//            auditEntry.put("response", response);
//
//            try {
//                var out = OBJECT_MAPPER.writeValueAsString(auditEntry);
//                System.out.println(out);
//            } catch (JsonProcessingException e) {
//                throw new RuntimeException(e);
//            }
//        }
    }
}
