package com.networknt.aws.lambda.body;

import java.util.List;

public class BodyConfig {

    private boolean enabled;
    private List<Transform> requestBodyTransform;
    private List<Transform> responseBodyTransform;

    public boolean isEnabled() {
        return enabled;
    }

    public List<Transform> getRequestBodyTransform() {
        return requestBodyTransform;
    }

    public List<Transform> getResponseBodyTransform() {
        return responseBodyTransform;
    }

    public static class Transform {

        public enum OperationType {
            ENCODE_NODE,
            SCOPE_TO_NODE,
            MOVE_NODE,
            REMOVE_NODE,
            ADD_NODE
        }

        private String path;
        private OperationType operationType;
        private String operation;

        public String getPath() {
            return path;
        }

        public OperationType getOperationType() {
            return operationType;
        }

        public String getOperation() {
            return operation;
        }
    }
}
