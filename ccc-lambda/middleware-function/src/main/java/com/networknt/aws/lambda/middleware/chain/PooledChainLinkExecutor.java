package com.networknt.aws.lambda.middleware.chain;

import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.LightLambdaExchange;
import com.networknt.aws.lambda.middleware.MiddlewareRunnable;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PooledChainLinkExecutor extends ThreadPoolExecutor {
    private final Logger LOG = LoggerFactory.getLogger(PooledChainLinkExecutor.class);
    private static final String CONFIG_NAME = "pooled-chain-executor";
    private static final PooledChainConfig CONFIG = (PooledChainConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PooledChainConfig.class);

    final Object lock = new Object();

    public PooledChainLinkExecutor() {
        super(CONFIG.getCorePoolSize(), CONFIG.getMaxPoolSize(), CONFIG.getKeepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    public void executeChain(LightLambdaExchange exchange, Chain chain) {

        if (!chain.isFinalized()) {
            LOG.error("Execution attempt on a chain that is not finalized! Call 'finalizeChain' before 'executeChain'");
            return;
        }

        for (var chainLinkGroup : chain.getGroupedChain()) {

            /* create workers & submit to queue */
            final var chainLinkWorkerGroup = this.createChainListWorkers(chainLinkGroup, exchange);
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
     * @param chainLinkGroup - sub-group of main chain
     * @param exchange
     * @return - List of thread workers for the tasks.
     */
    private ArrayList<ChainLinkWorker> createChainListWorkers(ArrayList<LambdaMiddleware> chainLinkGroup, LightLambdaExchange exchange) {
        final ArrayList<ChainLinkWorker> chainLinkWorkerGroup = new ArrayList<>();
        int linkNumber = 1;

        /* create a worker for each link in a group */
        for (var chainLink : chainLinkGroup) {
            LOG.debug("Creating thread for link '{}[{}]'.", chainLink.getClass().getName(), linkNumber++);
            var loggingContext = new ChainLinkWorker.AuditThreadContext(MDC.getCopyOfContextMap());
            var runnable = new MiddlewareRunnable(chainLink, exchange);
            var worker = new ChainLinkWorker(runnable, loggingContext);
            chainLinkWorkerGroup.add(worker);
        }

        return chainLinkWorkerGroup;
    }

    public void abortExecution() {
        synchronized (lock) {
            this.shutdownNow();
        }
    }

}
