package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.header.HeaderConfig;
import com.networknt.aws.lambda.middleware.Auditor;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.ChainLinkCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.ChainLinkReturn;
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
    private static final PooledChainConfig CONFIG = (PooledChainConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PooledChainConfig.class);

    private final LambdaEventWrapper lambdaEventWrapper;
    private final ChainDirection chainDirection;
    private final Chain chain = new Chain();
    private final AtomicInteger decrementCounter = new AtomicInteger(0);
    final Object lock = new Object();

    public PooledChainLinkExecutor(final LambdaEventWrapper lambdaEventWrapper, ChainDirection chainDirection) {
        super(CONFIG.getCorePoolSize(), CONFIG.getMaxPoolSize(), CONFIG.getKeepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.lambdaEventWrapper = lambdaEventWrapper;
        this.chainDirection = chainDirection;
    }

    public PooledChainLinkExecutor add(Class<? extends LambdaMiddleware> middleware) {

        if (!middleware.isAnnotationPresent(ChainProperties.class)) {
            LOG.error("Middleware '{}' is missing ChainProperties annotation.", middleware.getName());
        }

        try {
            var newClazz = middleware
                    .getConstructor(ChainLinkCallback.class, LambdaEventWrapper.class)
                    .newInstance(this.chainLinkCallback, this.lambdaEventWrapper);

            newClazz.setChainDirection(this.chainDirection);

            this.chain.addChainable(newClazz);
            int linkNumber = this.chain.getChainSize();

            if (LOG.isInfoEnabled())
                LOG.info("Created new middleware instance: {}[{}]", middleware.getAnnotation(ChainProperties.class).chainId(), linkNumber);

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

                if (LOG.isDebugEnabled())
                    LOG.debug("Creating thread for link '{}[{}]' in group '{}'.", chainLink.getClass().getAnnotation(ChainProperties.class).chainId(), linkNumber, groupNumber);

                chainLinkWorkerGroup.add(new ChainLinkWorker(chainLink, new ChainLinkWorker.AuditThreadContext(MDC.getCopyOfContextMap())));
                linkNumber++;
            }

            if (LOG.isDebugEnabled())
                LOG.debug("Setting decrement counter to: '{}'", this.decrementCounter.get());

            this.decrementCounter.getAndSet(chainLinkWorkerGroup.size());
            linkNumber = 1;

            /* submit each link in the group into a queue */
            for (var chainLinkWorker : chainLinkWorkerGroup) {

                if (LOG.isDebugEnabled())
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

            if (LOG.isDebugEnabled())
                LOG.debug("Decrement counter: '{}'", this.decrementCounter.get());
        }

        this.shutdown();
    }

    public LambdaEventWrapper getResolvedChainResult() {
        return this.lambdaEventWrapper;
    }

    private final ChainLinkCallback chainLinkCallback = new ChainLinkCallback() {
        @Override
        public void callback(final LambdaEventWrapper eventWrapper, ChainLinkReturn middlewareReturn) {
            chain.addChainableResult(middlewareReturn);

            if (middlewareReturn.getStatus() == ChainLinkReturn.Status.EXECUTION_FAILED)
                abortExecution();
        }

        @Override
        public void exceptionCallback(final LambdaEventWrapper eventWrapper, Throwable throwable) {

            if (throwable instanceof InterruptedException)
                chain.addChainableResult(new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_INTERRUPTED));

            else {
                LOG.error("Chain failed with exception: {}", throwable.getMessage(), throwable);
                chain.addChainableResult(new ChainLinkReturn(ChainLinkReturn.Status.EXECUTION_FAILED));
            }

            abortExecution();
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
