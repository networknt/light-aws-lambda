package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.config.Config;
import com.networknt.status.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PooledChainLinkExecutor extends ThreadPoolExecutor {
    private final Logger LOG = LoggerFactory.getLogger(PooledChainLinkExecutor.class);
    private static final String CONFIG_NAME = "pooled-chain-executor";
    private static final String MIDDLEWARE_THREAD_INTERRUPT = "ERR14003";
    private static final String MIDDLEWARE_UNHANDLED_EXCEPTION = "ERR14000";
    private static final PooledChainConfig CONFIG = (PooledChainConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PooledChainConfig.class);
    private final LightLambdaExchange lambdaEventWrapper;
    private final ChainDirection chainDirection;
    private final Chain chain;
    final Object lock = new Object();

    public PooledChainLinkExecutor(final LightLambdaExchange lambdaEventWrapper, ChainDirection chainDirection) {
        super(CONFIG.getCorePoolSize(), CONFIG.getMaxPoolSize(), CONFIG.getKeepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.lambdaEventWrapper = lambdaEventWrapper;
        this.chainDirection = chainDirection;
        this.chain = new Chain(CONFIG.isForceSynchronousExecution());
    }

    /**
     * Add to chain from string parameter
     *
     * @param className - class name in string format
     * @return - this
     */
    @SuppressWarnings("unchecked")
    public PooledChainLinkExecutor add(String className) {
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
    public PooledChainLinkExecutor add(Class<? extends LambdaMiddleware> middleware) {

        if (CONFIG.getMaxChainSize() <= this.chain.getChainSize()) {
            LOG.error("Chain is already at maxChainSize({}), cannot add anymore middleware to the chain.", CONFIG.getMaxChainSize());
            return this;
        }

        try {
            var newClazz = middleware.getConstructor(ChainLinkCallback.class, LightLambdaExchange.class)
                    .newInstance(this.chainLinkCallback, this.lambdaEventWrapper);

            newClazz.setChainDirection(this.chainDirection);

            //
            // TODO - assemble dynamo Db batch command to get all config attributes (instead of many smaller requests)
            //
            //newClazz.getAppConfigProfileConfigurations();

            this.chain.addChainable(newClazz);
            int linkNumber = this.chain.getChainSize();
            LOG.debug("Created new middleware instance: {}[{}]", middleware.getName(), linkNumber);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOG.error("failed to create class instance: {}", e.getMessage());

            if (CONFIG.isExitOnMiddlewareInstanceCreationFailure())
                throw new RuntimeException(e);

            else return this;
        }

        return this;
    }

    public void finalizeChain() {
        this.chain.setupGroupedChain();
    }

    public void executeChain() {

        if (!this.chain.isFinalized()) {
            LOG.error("Execution attempt on a chain that is not finalized! Call 'finalizeChain' before 'executeChain'");
            return;
        }

        for (var chainLinkGroup : this.chain.getGroupedChain()) {

            /* create workers & submit to queue */
            final var chainLinkWorkerGroup = this.createChainListWorkers(chainLinkGroup);
            final var chainLinkWorkerFutures = this.createChainLinkWorkerFutures(chainLinkWorkerGroup);

            /* await worker completion */
            this.awaitChainWorkerFutures(chainLinkWorkerFutures);
            if (this.isTerminating() || this.isTerminated() || this.isShutdown())
                break;

            chainLinkWorkerGroup.clear();
            chainLinkWorkerFutures.clear();
        }

        this.shutdown();
    }

    /**
     * Wait on future results.
     *
     * @param chainLinkWorkerFutures - list of futures from submitted tasks.
     */
    private void awaitChainWorkerFutures(final Collection<Future<?>> chainLinkWorkerFutures) {

        /* wait out the future results of the submitted tasks. */
        for (var chainLinkWorkerFuture : chainLinkWorkerFutures) {

            try {
                chainLinkWorkerFuture.get();

            } catch (InterruptedException | ExecutionException e) {
                LOG.error(e.getMessage(), e);
                return;
            }
        }
    }

    /**
     * Creates a collection of futures. One for each submitted task.
     *
     * @param chainLinkWorkerGroup  - list of threads for submission.
     * @return                      - list of futures for submitted tasks.
     */
    private Collection<Future<?>> createChainLinkWorkerFutures(ArrayList<ChainLinkWorker> chainLinkWorkerGroup) {

        final Collection<Future<?>> chainLinkWorkerFutures = new ArrayList<>();
        int linkNumber = 1;

        /* submit each link in the group into a queue */
        for (var chainLinkWorker : chainLinkWorkerGroup) {
            LOG.debug("Submitting link '{}' for execution.", linkNumber++);

            synchronized (lock) {

                if (!this.isShutdown() && !this.isTerminating() && !this.isTerminated())
                    chainLinkWorkerFutures.add(this.submit(chainLinkWorker));
            }
        }
        return chainLinkWorkerFutures;
    }

    /**
     * Creates the chain workers array to prepare for submission.
     *
     * @param chainLinkGroup    - sub-group of main chain
     * @return                  - List of thread workers for the tasks.
     */
    private ArrayList<ChainLinkWorker> createChainListWorkers(ArrayList<LambdaMiddleware> chainLinkGroup) {
        final ArrayList<ChainLinkWorker> chainLinkWorkerGroup = new ArrayList<>();
        int linkNumber = 1;

        /* create a worker for each link in a group */
        for (var chainLink : chainLinkGroup) {
            LOG.debug("Creating thread for link '{}[{}]'.", chainLink.getClass().getName(), linkNumber++);
            chainLinkWorkerGroup.add(new ChainLinkWorker(chainLink, new ChainLinkWorker.AuditThreadContext(MDC.getCopyOfContextMap())));
        }

        return chainLinkWorkerGroup;
    }

    private final ChainLinkCallback chainLinkCallback = new ChainLinkCallback() {

        @Override
        public void callback(final LightLambdaExchange eventWrapper, Status status) {
            PooledChainLinkExecutor.this.chain.addChainableResult(status);

            // TODO - change this to something more reliable than a string check
            if (status.getCode().startsWith("ERR")) {
                abortExecution();
            }

        }

        /* handles any generic throwable that occurred during middleware execution. */
        @Override
        public void exceptionCallback(final LightLambdaExchange eventWrapper, Throwable throwable) {
            PooledChainLinkExecutor.this.abortExecution();

            if (throwable instanceof InterruptedException) {
                LOG.error("Interrupted thread and cancelled middleware execution", throwable);
                PooledChainLinkExecutor.this.chain.addChainableResult(new Status(MIDDLEWARE_THREAD_INTERRUPT));

            } else {
                LOG.error("Middleware returned with unhandled exception.", throwable);
                PooledChainLinkExecutor.this.chain.addChainableResult(new Status(MIDDLEWARE_UNHANDLED_EXCEPTION));
            }

        }
    };

    private void abortExecution() {
        synchronized (lock) {
            this.shutdownNow();
        }
    }

    public Chain getChain() {
        return chain;
    }

    public LinkedList<Status> getChainLinkReturns() {
        return chain.getChainResults();
    }
}
