package com.networknt.aws.lambda.middleware;

import com.networknt.aws.lambda.middleware.chain.PooledChainLinkExecutor;
import org.junit.jupiter.api.Test;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
public class ChainTest {

    @Test
    public void testChain() {
        Chain testChain = new Chain(false, ChainDirection.REQUEST);
        TestSynchronousFailedResponseMiddleware testSynchronousFailedResponseMiddleware = new TestSynchronousFailedResponseMiddleware();
        testChain.addChainable(testSynchronousFailedResponseMiddleware);
        testChain.setupGroupedChain();

    }

}
