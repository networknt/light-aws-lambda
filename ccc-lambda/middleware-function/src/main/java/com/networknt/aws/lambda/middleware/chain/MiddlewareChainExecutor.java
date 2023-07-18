package com.networknt.aws.lambda.middleware.chain;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.LambdaMiddleware;
import com.networknt.aws.lambda.middleware.MiddlewareCallback;
import com.networknt.aws.lambda.middleware.response.MiddlewareReturn;
import com.networknt.aws.lambda.middleware.thread.MiddlewareThreadWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class MiddlewareChainExecutor {
    Logger LOG = LoggerFactory.getLogger(MiddlewareChainExecutor.class);
    final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent;
    protected final LambdaContext lambdaContext;
    protected final Chain chain = new Chain();
    final LinkedList<MiddlewareReturn<?>> middlewareReturns = new LinkedList<>();
    private ArrayList<MiddlewareThreadWorker> chainGroupExecutor;
    private final Object lock = new Object();
    volatile boolean continueExecuting = true;
    private final AtomicInteger decrementCounter = new AtomicInteger(0);

    public MiddlewareChainExecutor(final APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, final LambdaContext lambdaContext) {
        this.lambdaContext = lambdaContext;
        this.apiGatewayProxyRequestEvent = apiGatewayProxyRequestEvent;
    }

    public MiddlewareChainExecutor addChainLink(Class<? extends LambdaMiddleware> middleware) {

        try {

            var newClazz = middleware
                    .getConstructor(MiddlewareCallback.class, APIGatewayProxyRequestEvent.class, LambdaContext.class)
                    .newInstance(this.middlewareCallback, this.apiGatewayProxyRequestEvent, this.lambdaContext);


            this.chain.addChainable(newClazz);
            int linkNumber = this.chain.getChainSize();
            if (LOG.isInfoEnabled())
                LOG.info("-- Created new class instance: {}[{}]", newClazz.chainableId, linkNumber);

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            LOG.error("failed to create class: {}", e.getMessage());
        }

        return this;
    }

    public void finalizeChain() {
        this.chain.setupGroupedChain();
    }

    public void executeChain() {

        if (!this.chain.isFinalized())
            return;

        int groupNumber = 1;
        for (var linkGroup : this.chain.getGroupedChain()) {
            this.resetChainGroupExecutorAndDecrementCounter(linkGroup.size());

            int linkNumber = 1;
            for (var link : linkGroup) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Creating thread for link '{}' in group '{}'.", linkNumber, groupNumber);

                this.chainGroupExecutor.add(new MiddlewareThreadWorker(link, new MiddlewareThreadWorker.AuditThreadContext(MDC.getCopyOfContextMap())));
                linkNumber++;
            }

            linkNumber = 1;
            for (var executor : this.chainGroupExecutor) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Starting thread for link '{}' in group '{}'.", linkNumber, groupNumber);

                executor.start();
                linkNumber++;
            }


            linkNumber = 1;
            for (var executor : this.chainGroupExecutor) {

                if (LOG.isTraceEnabled())
                    LOG.trace("Joining thread for link '{}' in group '{}'.", linkNumber, groupNumber);

                try {
                    executor.join();
                } catch (InterruptedException e) {
                    LOG.error(e.getMessage());
                }

                linkNumber++;
            }

            if (this.decrementCounter.get() > 0)
                LOG.error("not all executors were able to finish the call back, or returned FAILED_STATUS.");

            if (!this.continueExecuting) {
                break;
            }

            groupNumber++;
        }

        this.chainGroupExecutor.clear();
    }

    private void resetChainGroupExecutorAndDecrementCounter(int decrementSize) {
        this.decrementCounter.getAndSet(decrementSize);
        this.chainGroupExecutor = new ArrayList<>();
    }

    private final MiddlewareCallback middlewareCallback = (proxyRequestEvent, context, middlewareReturn) -> {
        this.middlewareReturns.add(middlewareReturn);

        if (middlewareReturn.getStatus() == MiddlewareReturn.Status.EXECUTION_FAILED)
            this.abortExecution();

        else this.decrementCounter.decrementAndGet();
    };

    synchronized private void abortExecution() {

        synchronized (lock) {
            continueExecuting = false;
            for (var executor : this.chainGroupExecutor)
                executor.interrupt();
        }

    }

    public Chain getChain() {
        return chain;
    }

    public LinkedList<MiddlewareReturn<?>> getMiddlewareReturns() {
        return middlewareReturns;
    }
}
