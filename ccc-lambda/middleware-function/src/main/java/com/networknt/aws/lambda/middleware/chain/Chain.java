package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;

import java.util.ArrayList;
import java.util.LinkedList;

public class Chain {

    private final LinkedList<LambdaMiddleware> chain = new LinkedList<>();
    private final LinkedList<ArrayList<LambdaMiddleware>> groupedChain = new LinkedList<>();

    private final LinkedList<ChainLinkReturn> chainResults = new LinkedList<>();
    private boolean isFinalized;

    public Chain() {
        this.isFinalized = false;
    }

    protected void addChainable(LambdaMiddleware chainable) {
        if (!this.isFinalized) {
            this.chain.add(chainable);
        }
    }

    protected void addChainableResult(ChainLinkReturn result) {
        if (this.isFinalized) {
            this.chainResults.add(result);
        }
    }

    public boolean isFinalized() {
        return isFinalized;
    }

    public LinkedList<ArrayList<LambdaMiddleware>> getGroupedChain() {
        return groupedChain;
    }

    public int getChainSize() {
        return this.chain.size();
    }

    public void setupGroupedChain() {

        if (this.isFinalized)
            return;

        ArrayList<LambdaMiddleware> group = new ArrayList<>();
        for (var chainable : this.chain) {

            if (chainable.getClass().getAnnotation(ChainProperties.class).asynchronous()) {
                group.add(chainable);

            } else if (!chainable.getClass().getAnnotation(ChainProperties.class).asynchronous() && !group.isEmpty()) {
                this.groupedChain.add(group);
                group = new ArrayList<>();
                group.add(chainable);
                this.groupedChain.add(group);
                group = new ArrayList<>();

            } else if (!chainable.getClass().getAnnotation(ChainProperties.class).asynchronous() && group.isEmpty()) {
                group.add(chainable);
                this.groupedChain.add(group);
                group = new ArrayList<>();
            }
        }

        if (!group.isEmpty()) {
            this.groupedChain.add(group);
        }

        this.isFinalized = true;
    }

    public LinkedList<LambdaMiddleware> getChain() {
        return chain;
    }

    public LinkedList<ChainLinkReturn> getChainResults() {
        return chainResults;
    }
}
