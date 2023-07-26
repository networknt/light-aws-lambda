package com.networknt.aws.lambda.middleware.chain;

public class PooledChainConfig {
    private int maxPoolSize;
    private int corePoolSize;
    private long keepAliveTime;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }
}
