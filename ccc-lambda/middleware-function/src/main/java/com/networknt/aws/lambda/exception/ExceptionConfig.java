package com.networknt.aws.lambda.exception;

public class ExceptionConfig {

    private boolean showStatusCode;
    private boolean showMessage;
    private boolean showDescription;

    public boolean isShowStatusCode() {
        return showStatusCode;
    }

    public boolean isShowMessage() {
        return showMessage;
    }

    public boolean isShowDescription() {
        return showDescription;
    }
}
