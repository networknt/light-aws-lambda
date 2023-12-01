package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;

public class Chain {
    private static final Logger LOG = LoggerFactory.getLogger(Chain.class);
    private final LinkedList<LambdaMiddleware> chain = new LinkedList<>();
    private final LinkedList<ArrayList<LambdaMiddleware>> groupedChain = new LinkedList<>();

    private final boolean forceSynchronousExecution;

    private static final String CONFIG_NAME = "pooled-chain-executor";
    private final ChainDirection chainDirection;
    private static final PooledChainConfig CONFIG = (PooledChainConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PooledChainConfig.class);

    private boolean isFinalized;

    public Chain(boolean forceSynchronousExecution, ChainDirection chainDirection) {
        this.isFinalized = false;
        this.chainDirection = chainDirection;
        this.forceSynchronousExecution = forceSynchronousExecution;
    }

    public void addChainable(LambdaMiddleware chainable) {

        if (!this.isFinalized)
            this.chain.add(chainable);

        else LOG.error("Attempting to add chain link after chain has been finalized!");
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

    /**
     * Add to chain from string parameter
     *
     * @param className - class name in string format
     * @return - this
     */
    @SuppressWarnings("unchecked")
    public Chain add(String className) {
        try {

            if (Class.forName(className).getSuperclass().equals(LambdaMiddleware.class))
                return this.add((Class<? extends LambdaMiddleware>) Class.forName(className));

            else throw new RuntimeException(className + " is not a member of LambdaMiddleware...");

        } catch (ClassNotFoundException e) {
            LOG.error("Failed to find class with the name: {}", className);

            if (CONFIG.isExitOnMiddlewareInstanceCreationFailure())
                throw new RuntimeException(e);

            else return this;
        }
    }

    /**
     * Add to chain from class parameter
     * @param  middleware - middleware class
     * @return - this
     */
    public Chain add(Class<? extends LambdaMiddleware> middleware) {

        if (CONFIG.getMaxChainSize() <= this.chain.size()) {
            LOG.error("Chain is already at maxChainSize({}), cannot add anymore middleware to the chain.", CONFIG.getMaxChainSize());
            return this;
        }

        try {
            var newClazz = middleware.getConstructor()
                    .newInstance();

            newClazz.setChainDirection(this.chainDirection);
            newClazz.getCachedConfigurations();

            this.chain.add(newClazz);
            int linkNumber = this.chain.size();
            LOG.debug("Created new middleware instance: {}[{}]", middleware.getName(), linkNumber);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOG.error("failed to create class instance: {}", e.getMessage());

            if (CONFIG.isExitOnMiddlewareInstanceCreationFailure())
                throw new RuntimeException(e);

            else return this;
        }

        return this;
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

//    public LinkedList<Status> getChainResults() {
//        return chainResults;
//    }




}