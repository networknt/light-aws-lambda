package com.networknt.aws.lambda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LambdaInvokerConfig {
    private static final Logger logger = LoggerFactory.getLogger(LambdaInvokerConfig.class);

    public static final String CONFIG_NAME = "lambda-invoker";
    private static final String REGION = "region";
    private static final String ENDPOINT_OVERRIDE = "endpointOverride";
    private static final String API_CALL_TIMEOUT = "apiCallTimeout";
    private static final String API_CALL_ATTEMPT_TIMEOUT = "apiCallAttemptTimeout";
    private static final String LOG_TYPE = "logType";
    private static final String FUNCTIONS = "functions";
    private static final String METRICS_INJECTION = "metricsInjection";
    private static final String METRICS_NAME = "metricsName";
    private static final String MAX_RETRIES = "maxRetries";
    private static final String MAX_CONCURRENCY = "maxConcurrency";
    private static final String MAX_PENDING_CONNECTION_ACQUIRES = "maxPendingConnectionAcquires";
    private static final String CONNECTION_ACQUISITION_TIMEOUT = "connectionAcquisitionTimeout";

    private String region;
    private String endpointOverride;
    private int apiCallTimeout;
    private int apiCallAttemptTimeout;
    private String logType;
    private Map<String, String> functions;
    private boolean metricsInjection;
    private String metricsName;
    private int maxRetries;
    private int maxConcurrency;
    private int maxPendingConnectionAcquires;
    private int connectionAcquisitionTimeout;

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

    public int getApiCallTimeout() {
        return apiCallTimeout;
    }

    public void setApiCallTimeout(int apiCallTimeout) {
        this.apiCallTimeout = apiCallTimeout;
    }

    public int getApiCallAttemptTimeout() {
        return apiCallAttemptTimeout;
    }

    public void setApiCallAttemptTimeout(int apiCallAttemptTimeout) {
        this.apiCallAttemptTimeout = apiCallAttemptTimeout;
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

    public boolean isMetricsInjection() {
        return metricsInjection;
    }

    public void setMetricsInjection(boolean metricsInjection) {
        this.metricsInjection = metricsInjection;
    }

    public String getMetricsName() {
        return metricsName;
    }

    public void setMetricsName(String metricsName) {
        this.metricsName = metricsName;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
    }

    public int getMaxPendingConnectionAcquires() {
        return maxPendingConnectionAcquires;
    }

    public void setMaxPendingConnectionAcquires(int maxPendingConnectionAcquires) {
        this.maxPendingConnectionAcquires = maxPendingConnectionAcquires;
    }

    public int getConnectionAcquisitionTimeout() {
        return connectionAcquisitionTimeout;
    }

    public void setConnectionAcquisitionTimeout(int connectionAcquisitionTimeout) {
        this.connectionAcquisitionTimeout = connectionAcquisitionTimeout;
    }

    private final Config config;
    private Map<String, Object> mappedConfig;

    public LambdaInvokerConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private LambdaInvokerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigMap();
    }

    public static LambdaInvokerConfig load() {
        return new LambdaInvokerConfig();
    }

    public static LambdaInvokerConfig load(String configName) {
        return new LambdaInvokerConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigMap();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(REGION);
        if (object != null) {
            region = ((String)object);
        }
        object = mappedConfig.get(ENDPOINT_OVERRIDE);
        if (object != null) {
            endpointOverride = ((String) object);
        }
        object = mappedConfig.get(API_CALL_TIMEOUT);
        if (object != null) {
            apiCallTimeout = Config.loadIntegerValue(API_CALL_TIMEOUT, object);
        }
        object = mappedConfig.get(API_CALL_ATTEMPT_TIMEOUT);
        if (object != null) {
            apiCallAttemptTimeout = Config.loadIntegerValue(API_CALL_ATTEMPT_TIMEOUT, object);
        }
        object = mappedConfig.get(LOG_TYPE);
        if (object != null) {
            logType = ((String) object);
        }
        object = getMappedConfig().get(METRICS_INJECTION);
        if(object != null) {
            metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);
        }
        object = getMappedConfig().get(METRICS_NAME);
        if(object != null ) {
            metricsName = (String)object;
        }
        object = mappedConfig.get(MAX_RETRIES);
        if (object != null) {
            maxRetries = Config.loadIntegerValue(MAX_RETRIES, object);
        }
        object = mappedConfig.get(MAX_CONCURRENCY);
        if (object != null) {
            maxConcurrency = Config.loadIntegerValue(MAX_CONCURRENCY, object);
        }
        object = mappedConfig.get(MAX_PENDING_CONNECTION_ACQUIRES);
        if (object != null) {
            maxPendingConnectionAcquires = Config.loadIntegerValue(MAX_PENDING_CONNECTION_ACQUIRES, object);
        }
        object = mappedConfig.get(CONNECTION_ACQUISITION_TIMEOUT);
        if (object != null) {
            connectionAcquisitionTimeout = Config.loadIntegerValue(CONNECTION_ACQUISITION_TIMEOUT, object);
        }
    }

    private void setConfigMap() {
        if (mappedConfig.get(FUNCTIONS) != null) {
            Object object = mappedConfig.get(FUNCTIONS);
            functions = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("functions s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        functions = Config.getInstance().getMapper().readValue(s, new TypeReference<HashMap<String,String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the functions json with a map of string and string.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        functions.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                functions = (Map)object;
            } else {
                throw new ConfigException("functions must be a string string map.");
            }
        }
    }
}
