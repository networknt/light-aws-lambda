package com.networknt.aws.lambda.proxy;

import java.util.Map;

public class LambdaProxyConfig {

    private String region;
    private String endpointOverride;
    private String logType;
    private Map<String, String> functions;
    private boolean metricsInjection;
    private String metricsName;

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
}
