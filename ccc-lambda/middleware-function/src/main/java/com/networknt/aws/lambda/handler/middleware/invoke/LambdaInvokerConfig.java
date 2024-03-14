package com.networknt.aws.lambda.handler.middleware.invoke;

import java.util.List;
import java.util.Map;

public class LambdaInvokerConfig {

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

    public String getLambdaAppId() {
        return lambdaAppId;
    }

    public void setLambdaAppId(String lambdaAppId) {
        this.lambdaAppId = lambdaAppId;
    }

    public Map<String, String> getFunctions() {
        return functions;
    }

    public void setFunctions(Map<String, String> functions) {
        this.functions = functions;
    }
}
