package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.exception.ExceptionHandler;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LambdaEventWrapper;
import com.networknt.config.Config;
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
import java.util.concurrent.atomic.AtomicInteger;

public class PooledChainLinkExecutor extends ThreadPoolExecutor {

    private final Logger LOG = LoggerFactory.getLogger(PooledChainLinkExecutor.class);
    private static final String CONFIG_NAME = "pooled-chain-executor";
    private static final String MIDDLEWARE_THREAD_INTERRUPT = "ERR14003";
    private static final String MIDDLEWARE_UNHANDLED_EXCEPTION = "ERR14004";
    private static final PooledChainConfig CONFIG = (PooledChainConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PooledChainConfig.class);
    private final LambdaEventWrapper lambdaEventWrapper;
    private final String applicationId;
    private final String env;
    private final ChainDirection chainDirection;
    private final Chain chain = new Chain();
    private final AtomicInteger decrementCounter = new AtomicInteger(0);
    final Object lock = new Object();

    public PooledChainLinkExecutor(final LambdaEventWrapper lambdaEventWrapper, ChainDirection chainDirection, String applicationId, String env) {
        super(CONFIG.getCorePoolSize(), CONFIG.getMaxPoolSize(), CONFIG.getKeepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.lambdaEventWrapper = lambdaEventWrapper;
        this.chainDirection = chainDirection;
        this.env = env;
        this.applicationId = applicationId;
    }

    public PooledChainLinkExecutor add(Class<? extends LambdaMiddleware> middleware) {

        if (!middleware.isAnnotationPresent(ChainProperties.class)) {
            LOG.error("Middleware '{}' is missing ChainProperties annotation.", middleware.getName());
            return this;
        }

        if (middleware.getAnnotation(ChainProperties.class).id().equals(ChainProperties.DEFAULT_CHAIN_ID)) {
            LOG.error("Middleware '{}' does not have chainId defined!", middleware.getName());
            return this;
        }

        try {
            var newClazz = middleware.getConstructor(ChainLinkCallback.class, LambdaEventWrapper.class).newInstance(this.chainLinkCallback, this.lambdaEventWrapper);
            newClazz.setChainDirection(this.chainDirection);
            newClazz.initMiddlewareConfig(this.applicationId, this.env);
            this.chain.addChainable(newClazz);

            int linkNumber = this.chain.getChainSize();

            LOG.debug("Created new middleware instance: {}[{}]", middleware.getAnnotation(ChainProperties.class).id(), linkNumber);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOG.error("failed to create class: {}", e.getMessage());
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

        int groupNumber = 1;
        int linkNumber;

        for (var chainLinkGroup : this.chain.getGroupedChain()) {

            final ArrayList<ChainLinkWorker> chainLinkWorkerGroup = new ArrayList<>();
            final Collection<Future<?>> chainLinkWorkerFutures = new LinkedList<>();
            linkNumber = 1;

            /* create a worker for each link in a group */
            for (var chainLink : chainLinkGroup) {

                LOG.debug("Creating thread for link '{}[{}]' in group '{}'.", chainLink.getClass().getAnnotation(ChainProperties.class).id(), linkNumber, groupNumber);

                chainLinkWorkerGroup.add(new ChainLinkWorker(chainLink, new ChainLinkWorker.AuditThreadContext(MDC.getCopyOfContextMap())));
                linkNumber++;
            }

            LOG.debug("Setting decrement counter to: '{}'", this.decrementCounter.get());

            this.decrementCounter.getAndSet(chainLinkWorkerGroup.size());
            linkNumber = 1;

            /* submit each link in the group into a queue */
            for (var chainLinkWorker : chainLinkWorkerGroup) {

                LOG.debug("Submitting link '{}' in group '{}' for execution.", linkNumber, groupNumber);

                synchronized (lock) {

                    if (!this.isShutdown() && !this.isTerminating())
                        chainLinkWorkerFutures.add(this.submit(chainLinkWorker));

                }

                this.decrementCounter.decrementAndGet();
                linkNumber++;
            }


            /* wait out the future results of the submitted tasks. */
            for (var chainLinkWorkerFuture : chainLinkWorkerFutures) {

                try {
                    chainLinkWorkerFuture.get();

                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            if (this.isTerminating() || this.isTerminated() || this.isShutdown())
                break;

            chainLinkWorkerGroup.clear();
            chainLinkWorkerFutures.clear();

            LOG.trace("Decrement counter: '{}'", this.decrementCounter.get());
        }

        this.shutdown();
    }

    public LinkedList<ChainLinkReturn> getResolvedChainResults() {
        return this.chain.getChainResults();
    }

    private final ChainLinkCallback chainLinkCallback = new ChainLinkCallback() {
        @Override
        public void callback(final LambdaEventWrapper eventWrapper, ChainLinkReturn middlewareReturn) {
            chain.addChainableResult(middlewareReturn);

            if (middlewareReturn.getStatus() == ChainLinkReturn.Status.EXECUTION_FAILED) {
                abortExecution();
                ExceptionHandler.handle(middlewareReturn);
            }

        }

        /* handles any generic throwable that occurred during middleware execution. */
        @Override
        public void exceptionCallback(final LambdaEventWrapper eventWrapper, Throwable throwable) {
            abortExecution();
            if (throwable instanceof InterruptedException)
                ExceptionHandler.handle(new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_INTERRUPTED, MIDDLEWARE_THREAD_INTERRUPT));

            else {
                LOG.error("Chain failed with exception: {}", throwable.getMessage(), throwable);
                ExceptionHandler.handle(new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED, MIDDLEWARE_UNHANDLED_EXCEPTION));
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

    public LinkedList<ChainLinkReturn> getChainLinkReturns() {
        return chain.getChainResults();
    }
}
