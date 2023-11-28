package com.networknt.aws.lambda.middleware.chain;

public abstract class Chainable {
    protected final boolean audited;
    protected final boolean asynchronous;
    protected final boolean continueOnFailure;
    private ChainDirection chainDirection;

    protected Chainable(boolean audited, boolean asynchronous, boolean continueOnFailure) {
        this.audited = audited;
        this.asynchronous = asynchronous;
        this.continueOnFailure = continueOnFailure;
    }

    public ChainDirection getChainDirection() {
        return chainDirection;
    }

    protected void setChainDirection(ChainDirection chainDirection) {
        this.chainDirection = chainDirection;
    }
}
