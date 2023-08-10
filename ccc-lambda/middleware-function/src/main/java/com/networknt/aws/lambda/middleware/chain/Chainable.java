package com.networknt.aws.lambda.middleware.chain;

public abstract class Chainable {

    protected final boolean audited;
    protected final boolean asynchronous;
    protected final boolean continueOnFailure;

    protected final ChainLinkCallback middlewareCallback;
    private ChainDirection chainDirection;

    protected Chainable(boolean audited, boolean asynchronous, boolean continueOnFailure, final ChainLinkCallback middlewareCallback) {
        this.audited = audited;
        this.asynchronous = asynchronous;
        this.continueOnFailure = continueOnFailure;
        this.middlewareCallback = middlewareCallback;
    }

    public ChainDirection getChainDirection() {
        return chainDirection;
    }

    protected void setChainDirection(ChainDirection chainDirection) {
        this.chainDirection = chainDirection;
    }
}
