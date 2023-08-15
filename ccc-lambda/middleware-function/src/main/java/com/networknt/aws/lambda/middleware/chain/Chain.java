package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;

public class Chain {
    private static final Logger LOG = LoggerFactory.getLogger(Chain.class);
    private final LinkedList<LambdaMiddleware> chain = new LinkedList<>();
    private final LinkedList<ArrayList<LambdaMiddleware>> groupedChain = new LinkedList<>();
    private final LinkedList<Status> chainResults = new LinkedList<>();
    private final boolean forceSynchronousExecution;
    private boolean isFinalized;

    public Chain(boolean forceSynchronousExecution) {
        this.isFinalized = false;
        this.forceSynchronousExecution = forceSynchronousExecution;
    }

    protected void addChainable(LambdaMiddleware chainable) {

        if (!this.isFinalized)
            this.chain.add(chainable);

        else LOG.error("Attempting to add chain link after chain has been finalized!");
    }

    protected void addChainableResult(Status result) {

        if (this.isFinalized)
            this.chainResults.add(result);

        else LOG.error("Attempting to add link result before chain has been finalized.");
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

        var group = new ArrayList<LambdaMiddleware>();
        for (var chainable : this.chain) {

            if (this.forceSynchronousExecution) {
                this.cutGroup(group, chainable);
                group = new ArrayList<>();

            } else if (this.isMiddlewareAsynchronous(chainable)) {
                group.add(chainable);

            } else if (!this.isMiddlewareAsynchronous(chainable) && !group.isEmpty()) {
                this.groupedChain.add(group);
                group = new ArrayList<>();
                this.cutGroup(group, chainable);
                group = new ArrayList<>();

            } else if (!this.isMiddlewareAsynchronous(chainable) && group.isEmpty()) {
                this.cutGroup(group, chainable);
                group = new ArrayList<>();
            }
        }

        if (!group.isEmpty()) {
            this.groupedChain.add(group);
        }

        this.isFinalized = true;
    }

    private void cutGroup(ArrayList<LambdaMiddleware> group, LambdaMiddleware chainable) {
        group.add(chainable);
        this.groupedChain.add(group);
    }

    private boolean isMiddlewareAsynchronous(LambdaMiddleware chainable) {
        LOG.trace("Checking if chainable class '{}' is asynchronous.", chainable.getClass());
        return chainable.asynchronous;
    }

    public LinkedList<LambdaMiddleware> getChain() {
        return chain;
    }

    public LinkedList<Status> getChainResults() {
        return chainResults;
    }
}
