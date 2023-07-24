package com.networknt.aws.lambda.header;

public class HeaderChange {
    protected String HeaderKey;
    protected ChangeDesc changeDesc;

    public static class ChangeDesc {

        enum ChangeType {
            ADD,
            REMOVE,
            MODIFY
        }

        protected ChangeType changeType;
        protected String value;

    }
}
