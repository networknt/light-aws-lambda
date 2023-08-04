package com.networknt.aws.lambda.middleware.header;

import java.util.List;

public class HeaderConfig {
    private boolean enabled;
    private List<HeaderChange> requestHeader;
    private List<HeaderChange> responseHeader;

    public boolean isEnabled() {
        return enabled;
    }

    public List<HeaderChange> getRequestHeader() {
        return requestHeader;
    }

    public List<HeaderChange> getResponseHeader() {
        return responseHeader;
    }

    @Override
    public String toString() {
        return "HeaderConfig{" +
                "enabled=" + enabled +
                ", requestHeader=" + requestHeader.toString() +
                ", responseHeader=" + responseHeader.toString() +
                '}';
    }

    public static class HeaderChange {
        private String headerKey;
        private ChangeDescriptor changeDescriptor;

        public String getHeaderKey() {
            return headerKey;
        }

        public ChangeDescriptor getChangeDescriptor() {
            return changeDescriptor;
        }

        @Override
        public String toString() {
            return "HeaderChange{" +
                    "headerKey='" + headerKey + '\'' +
                    ", changeDescriptor=" + changeDescriptor.toString() +
                    '}';
        }
    }

    public static class ChangeDescriptor {

        enum ChangeType {
            ADD,
            REMOVE,
            APPEND,
            PREPEND,
            REPLACE
        }

        private ChangeType changeType;
        private String value;

        public ChangeType getChangeType() {
            return changeType;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "ChangeDescriptor{" +
                    "changeType=" + changeType +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
