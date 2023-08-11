package com.networknt.aws.lambda.status;

import java.util.Map;

public class LambdaStatusConfig {
    private boolean showStatusCode;
    private boolean showMessage;
    private boolean showDescription;

    private boolean showMetadata;

    private Map<String, StatusDef> status;

    public boolean isShowStatusCode() {
        return showStatusCode;
    }

    public boolean isShowMessage() {
        return showMessage;
    }

    public boolean isShowDescription() {
        return showDescription;
    }

    public boolean isShowMetadata() {
        return showMetadata;
    }

    public Map<String, StatusDef> getStatus() {
        return status;
    }

    public static class StatusDef {
        private int statusCode;
        private String code;
        private String message;
        private String severity;
        private String description;

        public int getStatusCode() {
            return statusCode;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getSeverity() {
            return severity;
        }

        public String getDescription() {
            return description;
        }
    }
}
