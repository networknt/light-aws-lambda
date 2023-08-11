package com.networknt.aws.lambda.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;

import java.util.Map;

public class LambdaStatus {
    private static final String CONFIG_NAME = "lambda-status";
    private static final LambdaStatusConfig CONFIG = (LambdaStatusConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, LambdaStatusConfig.class);
    private static final String MIDDLEWARE_OK = "SUC14200";
    private static final String LAMBDA_NATIVE_HANDLER_DISABLED = "ERR14001";

    public enum Status
    {
        EXECUTION_SUCCESS,
        EXECUTION_FAILED,
        EXECUTION_INTERRUPTED
    }

    private final Status status;
    private final String code;
    private int statusCode;
    private String message;
    private String description;
    private String severity;
    private Map<String, String> metadata;

    public LambdaStatus(Status status, String code) {
        this.code = code;
        this.status = status;
        this.resolveStatus(this.code);
    }

    public LambdaStatus(Status status, String code, Map<String, String> metadata) {
        this.code = code;
        this.status = status;
        this.metadata = metadata;
        this.resolveStatus(this.code);
    }

    private void resolveStatus(final String statusCode) {
        if (CONFIG.getStatus().containsKey(statusCode)) {
            var statusDef = CONFIG.getStatus().get(statusCode);
            this.message = statusDef.getMessage();
            this.description = statusDef.getDescription();
            this.statusCode = statusDef.getStatusCode();
            this.severity = statusDef.getSeverity();
        }
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getSeverity() {
        return this.severity;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Status getStatus() {
        return status;
    }

    public String toStringConditionally() {
        var sb = new StringBuilder();

        sb.append("{").append("\"statusCode\":").append(getCode()).append(",\"code\":\"").append(getStatusCode());

        if (CONFIG.isShowMessage())
            sb.append("\",\"message\":\"").append(getMessage());

        if (CONFIG.isShowDescription())
            sb.append("\",\"description\":\"").append(getDescription());

        if (CONFIG.isShowMetadata() && getMetadata() != null) {

            try {
                sb.append("\",\"metadata\":").append(Config.getInstance().getMapper().writeValueAsString(getMetadata()));

            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            sb.append(",\"severity\":\"").append(getSeverity());

        } else sb.append("\",\"severity\":\"").append(getSeverity());

        sb.append("\"}");

        return sb.toString();
    }

    public static LambdaStatus disabledMiddlewareReturn() {
        return new LambdaStatus(Status.EXECUTION_FAILED, LAMBDA_NATIVE_HANDLER_DISABLED);
    }

    public static LambdaStatus successMiddlewareReturn() {
        return new LambdaStatus(Status.EXECUTION_SUCCESS, MIDDLEWARE_OK);
    }
}
