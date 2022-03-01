package com.networknt.aws.lambda;

import java.util.Map;

public class LambdaInvokerConfig {
    public static final String CONFIG_NAME = "lambda-invoker";

    private String region;
    private String endpointOverride;
    private String logType;
    private Map<String, String> functions;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getEndpointOverride() {
        return endpointOverride;
    }

    public void setEndpointOverride(String endpointOverride) {
        this.endpointOverride = endpointOverride;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }
}
