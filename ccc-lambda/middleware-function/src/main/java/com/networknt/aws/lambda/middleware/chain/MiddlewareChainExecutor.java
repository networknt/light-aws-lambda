package com.networknt.aws.lambda.middleware.chain;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.MiddlewareCallback;
import com.networknt.aws.lambda.middleware.payload.LambdaEventWrapper;
import com.networknt.aws.lambda.middleware.payload.MiddlewareReturn;
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

public class MiddlewareChainExecutor extends ThreadPoolExecutor {
    private final Logger LOG = LoggerFactory.getLogger(MiddlewareChainExecutor.class);
    private final static int MAX_POOL_SIZE = 10;
    private final static long KEEP_ALIVE_TIME = 0L;
    private final LambdaEventWrapper lambdaEventWrapper;
    private final Chain chain = new Chain();
    private final LinkedList<MiddlewareReturn<?>> middlewareReturns = new LinkedList<>();
    private final AtomicInteger decrementCounter = new AtomicInteger(0);
    final Object lock = new Object();

    public MiddlewareChainExecutor(final LambdaEventWrapper lambdaEventWrapper) {
        super(MAX_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        this.lambdaEventWrapper = lambdaEventWrapper;
    }

    @SuppressWarnings("rawtypes")
    public MiddlewareChainExecutor add(Class<? extends LambdaMiddleware> middleware) {

        try {
            var newClazz = middleware
                    .getConstructor(MiddlewareCallback.class, LambdaEventWrapper.class)
                    .newInstance(this.chainMiddlewareCallback, this.lambdaEventWrapper);

            this.chain.addChainable(newClazz);
            int linkNumber = this.chain.getChainSize();

            if (LOG.isInfoEnabled())
                LOG.info("Created new middleware instance: {}[{}]", newClazz.chainableId, linkNumber);

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

        for (var linkGroup : this.chain.getGroupedChain()) {

            final ArrayList<MiddlewareThreadWorker> middlewareGroup = new ArrayList<>();
            final Collection<Future<?>> middlewareGroupFutures = new LinkedList<>();
            linkNumber = 1;

            for (var link : linkGroup) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Creating thread for link '{}' in group '{}'.", linkNumber, groupNumber);

                middlewareGroup.add(new MiddlewareThreadWorker(link, new MiddlewareThreadWorker.AuditThreadContext(MDC.getCopyOfContextMap())));
                linkNumber++;
            }

            this.decrementCounter.getAndSet(middlewareGroup.size());
            linkNumber = 1;

            for (var executor : middlewareGroup) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Starting thread for link '{}' in group '{}'.", linkNumber, groupNumber);

                synchronized (lock) {

                    if (!this.isShutdown() && !this.isTerminating())
                        middlewareGroupFutures.add(this.submit(executor));

                }

                this.decrementCounter.decrementAndGet();
                linkNumber++;
            }

            if (this.isShutdown() || this.isTerminated() || this.decrementCounter.get() != 0)
                return;

            for (var future : middlewareGroupFutures) {

                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }

            middlewareGroup.clear();
            middlewareGroupFutures.clear();
        }
    }

    private final MiddlewareCallback chainMiddlewareCallback = new MiddlewareCallback() {
        @Override
        public void callback(final LambdaEventWrapper eventWrapper, MiddlewareReturn<?> middlewareReturn) {
            middlewareReturns.add(middlewareReturn);

            if (middlewareReturn.getStatus() == MiddlewareReturn.Status.EXECUTION_FAILED)
                abortExecution();
        }

        @Override
        public void exceptionCallback(final LambdaEventWrapper eventWrapper, Throwable throwable) {
            middlewareReturns.add(new MiddlewareReturn<>(throwable, MiddlewareReturn.Status.EXECUTION_FAILED));
            abortExecution();
        }
    };

    private void abortExecution() {
        synchronized (lock) {
            this.shutdown();
        }
    }

    public Chain getChain() {
        return chain;
    }

    public LinkedList<MiddlewareReturn<?>> getMiddlewareReturns() {
        return middlewareReturns;
    }
}
