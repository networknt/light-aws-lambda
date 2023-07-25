package com.networknt.aws.lambda.header;

import java.util.List;

public class HeaderConfig {

    public static final String CONFIG_NAME = "header";

    boolean enabled;
    List<HeaderChange> requestHeader;
    List<HeaderChange> responseHeader;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<HeaderChange> getRequestHeader() {
        return requestHeader;
    }

    public void setRequestHeader(List<HeaderChange> requestHeader) {
        this.requestHeader = requestHeader;
    }

    public List<HeaderChange> getResponseHeader() {
        return responseHeader;
    }

    public void setResponseHeader(List<HeaderChange> responseHeader) {
        this.responseHeader = responseHeader;
    }

    public static class HeaderChange {
        private String headerKey;
        private ChangeDescriptor changeDescriptor;

        public String getHeaderKey() {
            return headerKey;
        }

        public void setHeaderKey(String headerKey) {
            this.headerKey = headerKey;
        }

        public ChangeDescriptor getChangeDescriptor() {
            return changeDescriptor;
        }

        public void setChangeDescriptor(ChangeDescriptor changeDesc) {
            this.changeDescriptor = changeDesc;
        }
    }

    public static class ChangeDescriptor {

        enum ChangeType {
            ADD,
            REMOVE,
            APPEND,
            PREPEND
        }

        private ChangeType changeType;
        private String value;

        public ChangeType getChangeType() {
            return changeType;
        }

        public void setChangeType(ChangeType changeType) {
            this.changeType = changeType;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
