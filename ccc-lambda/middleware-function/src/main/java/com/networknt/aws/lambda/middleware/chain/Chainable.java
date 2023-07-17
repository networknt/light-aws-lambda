package com.networknt.aws.lambda.middleware.chain;

public abstract class Chainable {
    protected final String chainableId;
    protected final boolean isSynchronous;

    protected Chainable(final String chainableId, final boolean isSynchronous) {
        this.chainableId = chainableId;
        this.isSynchronous = isSynchronous;
    }

    public String getChainableId() {
        return chainableId;
    }

    public boolean isSynchronous() {
        return isSynchronous;
    }

}
