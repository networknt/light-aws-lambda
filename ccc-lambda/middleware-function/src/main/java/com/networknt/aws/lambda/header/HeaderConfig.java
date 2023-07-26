package com.networknt.aws.lambda.header;

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

    public static class HeaderChange {
        private String headerKey;
        private ChangeDescriptor changeDescriptor;

        public String getHeaderKey() {
            return headerKey;
        }

        public ChangeDescriptor getChangeDescriptor() {
            return changeDescriptor;
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

        public String getValue() {
            return value;
        }
    }
}
