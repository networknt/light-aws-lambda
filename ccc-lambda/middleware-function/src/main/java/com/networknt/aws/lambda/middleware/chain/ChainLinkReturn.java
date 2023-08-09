package com.networknt.aws.lambda.middleware.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;

import java.util.Map;

public class ChainLinkReturn {
    private static final String CONFIG_NAME = "status";
    private static final Map<String, Object> STATUS = Config.getInstance().getJsonMapConfig(CONFIG_NAME);
    private static final String MIDDLEWARE_OK = "SUC14200";
    private static final String LAMBDA_NATIVE_HANDLER_DISABLED = "ERR14001";

    public enum Status
    {
        EXECUTION_SUCCESS,
        EXECUTION_FAILED,
        EXECUTION_INTERRUPTED
    }

    private final Status status;
    private final String statusCode;
    private int code;
    private String message;
    private String description;
    private Map<String, String> metadata;

    public ChainLinkReturn(Status status, String code) {
        this.statusCode = code;
        this.status = status;
        this.resolveStatus(this.statusCode);
    }

    @SuppressWarnings("unchecked")
    private void resolveStatus(final String statusCode) {
        if (STATUS.containsKey(statusCode)) {
            var statusCodeMap = (Map<String, Object>) STATUS.get(statusCode);
            this.message = (String) statusCodeMap.get("message");
            this.description = (String) statusCodeMap.get("description");
            this.code = (Integer) statusCodeMap.get("code");
        }
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public int getCode() {
        return code;
    }

    // TODO - is this needed?
    public String getSeverity() {
        return "";
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Status getStatus() {
        return status;
    }

    public String toStringConditionally(boolean showMessage, boolean showDescription, boolean showMetadata) {
        var sb = new StringBuilder();

        sb.append("{").append("\"statusCode\":").append(getStatusCode()).append(",\"code\":\"").append(getCode());

        if (showMessage)
            sb.append("\",\"message\":\"").append(getMessage());

        if (showDescription)
            sb.append("\",\"description\":\"").append(getDescription());

        if (showMetadata && getMetadata() != null) {

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

    public static ChainLinkReturn disabledMiddlewareReturn() {
        return new ChainLinkReturn(Status.EXECUTION_FAILED, LAMBDA_NATIVE_HANDLER_DISABLED);
    }

    public static ChainLinkReturn successMiddlewareReturn() {
        return new ChainLinkReturn(Status.EXECUTION_SUCCESS, MIDDLEWARE_OK);
    }
}
