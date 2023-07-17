package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;

import java.util.ArrayList;
import java.util.LinkedList;

public class Chain {

    protected final LinkedList<LambdaMiddleware> chain = new LinkedList<>();
    protected final LinkedList<ArrayList<LambdaMiddleware>> groupedChain = new LinkedList<>();
    private boolean isFinalized;

    public void addChainable(LambdaMiddleware chainable) {
        if (!this.isFinalized) {
            this.chain.add(chainable);
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

            if (!chainable.isSynchronous()) {
                group.add(chainable);

            } else if (chainable.isSynchronous() && !group.isEmpty()) {
                this.groupedChain.add(group);
                group = new ArrayList<>();
                group.add(chainable);
                this.groupedChain.add(group);
                group = new ArrayList<>();

            } else if (chainable.isSynchronous() && group.isEmpty()) {
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


}
