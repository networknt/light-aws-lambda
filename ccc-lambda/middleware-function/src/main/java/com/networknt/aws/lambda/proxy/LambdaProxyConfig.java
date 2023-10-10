package com.networknt.aws.lambda.proxy;

import java.util.List;
import java.util.Map;

public class LambdaProxyConfig {
    private String region;
    private String endpointOverride;
    private String logType;
    private String lambdaAppId;
    private String env;
    private Map<String, String> functions;
    private boolean metricsInjection;
    private String metricsName;
    private List<String> requestChain;
    private List<String> responseChain;
    private boolean enableDynamoDbCache;

    public List<String> getRequestChain() {
        return requestChain;
    }

    public List<String> getResponseChain() {
        return responseChain;
    }

    public String getRegion() {
        return region;
    }

    public String getEndpointOverride() {
        return endpointOverride;
    }

    public String getLogType() {
        return logType;
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public boolean isMetricsInjection() {
        return metricsInjection;
    }

    public String getMetricsName() {
        return metricsName;
    }

    public String getLambdaAppId() {
        return lambdaAppId;
    }

    public String getEnv() {
        return env;
    }

    public boolean isEnableDynamoDbCache() {
        return enableDynamoDbCache;
    }
}
