package com.networknt.aws.lambda.middleware.chain;

public class ChainLinkReturn {

    public enum Status
    {
        EXECUTION_SUCCESS,
        EXECUTION_FAILED,
        EXECUTION_INTERRUPTED,
        DISABLED
    }

    private final Status status;
    private String message;
    private String additionalDetails;

    public ChainLinkReturn(Status status) {
        this.status = status;
    }

    public ChainLinkReturn(Status status, String message) {
        this.status = status;
        this.message = message;
        this.additionalDetails = "";
    }

    public ChainLinkReturn(Status status, String message, String additionalDetails) {
        this.status = status;
        this.message = message;
        this.additionalDetails = additionalDetails;
    }

    public String getMessage() {
        return message;
    }

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public Status getStatus() {
        return status;
    }

}
