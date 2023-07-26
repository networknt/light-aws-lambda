package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.ChainLinkCallback;

public abstract class Chainable {
    protected final ChainLinkCallback middlewareCallback;
    private ChainDirection chainDirection;

    protected Chainable(final ChainLinkCallback middlewareCallback) {
        this.middlewareCallback = middlewareCallback;
    }

    public ChainDirection getChainDirection() {
        return chainDirection;
    }

    protected void setChainDirection(ChainDirection chainDirection) {
        this.chainDirection = chainDirection;
    }
}
