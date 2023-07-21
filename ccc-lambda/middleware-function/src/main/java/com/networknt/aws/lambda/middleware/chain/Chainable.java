package com.networknt.aws.lambda.middleware.chain;

public abstract class Chainable {
    protected final String chainableId;
    protected final boolean isSynchronous;
    protected final boolean isAudited;

    protected Chainable(final String chainableId, final boolean isSynchronous, final boolean isAudited) {
        this.chainableId = chainableId;
        this.isSynchronous = isSynchronous;
        this.isAudited = isAudited;
    }

    public String getChainableId() {
        return chainableId;
    }

    public boolean isSynchronous() {
        return isSynchronous;
    }

}
