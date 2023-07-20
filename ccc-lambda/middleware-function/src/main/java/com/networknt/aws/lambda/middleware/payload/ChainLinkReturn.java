package com.networknt.aws.lambda.middleware.payload;

import java.util.Map;

public class ChainLinkReturn<T> extends GenericResponse<T> {

    public enum Status
    {
        EXECUTION_SUCCESS,
        EXECUTION_FAILED
    }

    protected final Status status;
    protected final T response;

    public ChainLinkReturn(T response, Status status) {
        this.response = response;
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public T getResponse() {
        return response;
    }

    @SuppressWarnings("rawtypes")
    public Class getResponseType() {
        if (response != null)
            return this.reflectClassType();

        else return Object.class;
    }
}
