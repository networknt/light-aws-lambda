package com.networknt.aws.lambda;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.config.schema.IntegerField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.MapField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.server.ModuleRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// <<< REQUIRED ANNOTATION FOR SCHEMA GENERATION >>>
@ConfigSchema(
        configKey = "lambda-invoker",
        configName = "lambda-invoker",
        configDescription = "Configuration for AWS Lambda function invocation client.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML}
)
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

    // --- Annotated Fields ---
    private final Map<String, Object> mappedConfig;
    private static LambdaInvokerConfig instance;

    @StringField(
            configFieldName = REGION,
            externalizedKeyName = REGION,
            description = "The aws region that is used to create the LambdaClient.",
            defaultValue = "us-east-1"
    )
    private String region;

    @StringField(
            configFieldName = ENDPOINT_OVERRIDE,
            externalizedKeyName = ENDPOINT_OVERRIDE,
            description = "endpoint override if for lambda function deployed in virtual private cloud. Here is an example.\n" +
                    "https://vpce-0012C939329d982-tk8ps.lambda.ca-central-1.vpce.amazonaws.com\n"
    )
    private String endpointOverride;

    @IntegerField(
            configFieldName = API_CALL_TIMEOUT,
            externalizedKeyName = API_CALL_TIMEOUT,
            description = "Api call timeout in milliseconds. This sets the amount of time for the entire execution, including all retry attempts.",
            defaultValue = "60000"
    )
    private int apiCallTimeout;

    @IntegerField(
            configFieldName = API_CALL_ATTEMPT_TIMEOUT,
            externalizedKeyName = API_CALL_ATTEMPT_TIMEOUT,
            description = "Api call attempt timeout in milliseconds. This sets the amount of time for each individual attempt.",
            defaultValue = "20000"
    )
    private int apiCallAttemptTimeout;

    @IntegerField(
            configFieldName = MAX_RETRIES,
            externalizedKeyName = MAX_RETRIES,
            description = "The maximum number of retries for the Lambda function invocation. Default is 2, which equals to 3 max attempts.\n" +
                    "Set to 0 to disable retries so that the Lambda function is invoked only once.\n",
            defaultValue = "2"
    )
    private int maxRetries;

    @IntegerField(
            configFieldName = MAX_CONCURRENCY,
            externalizedKeyName = MAX_CONCURRENCY,
            description = "The maximum number of concurrent requests that can be made to Lambda. Default is 50.",
            defaultValue = "50"
    )
    private int maxConcurrency;

    @IntegerField(
            configFieldName = MAX_PENDING_CONNECTION_ACQUIRES,
            externalizedKeyName = MAX_PENDING_CONNECTION_ACQUIRES,
            description = "The maximum number of pending acquires allowed. Default is 10000.",
            defaultValue = "10000"
    )
    private int maxPendingConnectionAcquires;

    @IntegerField(
            configFieldName = CONNECTION_ACQUISITION_TIMEOUT,
            externalizedKeyName = CONNECTION_ACQUISITION_TIMEOUT,
            description = "The amount of time to wait when acquiring a connection from the pool before timing out in seconds. Default is 10 seconds.",
            defaultValue = "10"
    )
    private int connectionAcquisitionTimeout;

    @StringField(
            configFieldName = LOG_TYPE,
            externalizedKeyName = LOG_TYPE,
            description = "The LogType of the execution log of Lambda. Set Tail to include and None to not include.",
            defaultValue = "Tail"
    )
    private String logType;

    @MapField(
            configFieldName = FUNCTIONS,
            externalizedKeyName = FUNCTIONS,
            description = "Mapping of the endpoints to Lambda functions (Map of String to String).",
            valueType = String.class
    )
    private Map<String, String> functions; // Keep as Map<String, String>

    @BooleanField(
            configFieldName = METRICS_INJECTION,
            externalizedKeyName = METRICS_INJECTION,
            description = "When LambdaFunctionHandler is used in the light-gateway, it can collect the metrics info for the total\n" +
                    "response time of the downstream Lambda functions. With this value injected, users can quickly determine\n" +
                    "how much time the light-gateway handlers spend and how much time the downstream Lambda function spends,\n" +
                    "including the network latency. By default, it is false, and metrics will not be collected and injected\n" +
                    "into the metrics handler configured in the request/response chain.\n",
            defaultValue = "false"
    )
    private boolean metricsInjection;

    @StringField(
            configFieldName = METRICS_NAME,
            externalizedKeyName = METRICS_NAME,
            description = "When the metrics info is injected into the metrics handler, we need to pass a metric name to it so that\n" +
                    "the metrics info can be categorized in a tree structure under the name. By default, it is lambda-response,\n" +
                    "and users can change it.\n",
            defaultValue = "lambda-response"
    )
    private String metricsName;


    // --- Constructor and Loading Logic ---

    public LambdaInvokerConfig() {
        this(CONFIG_NAME);
    }

    private LambdaInvokerConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
        setConfigMap();
    }

    public static LambdaInvokerConfig load() {
        return load(CONFIG_NAME);
    }

    public static LambdaInvokerConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (LambdaInvokerConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new LambdaInvokerConfig(configName);
                ModuleRegistry.registerModule(CONFIG_NAME, LambdaInvokerConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(CONFIG_NAME), null);
                return instance;
            }
        }
        return new LambdaInvokerConfig(configName);
    }



    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    // --- Getters and Setters (Original Methods) ---

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

    // --- Private Config Loader ---

    private void setConfigData() {
        // Load simple annotated fields using standard Config loader/mapper logic
        Object object = mappedConfig.get(REGION);
        if (object != null) region = (String)object;

        object = mappedConfig.get(ENDPOINT_OVERRIDE);
        if (object != null) endpointOverride = (String) object;

        object = mappedConfig.get(API_CALL_TIMEOUT);
        if (object != null) apiCallTimeout = Config.loadIntegerValue(API_CALL_TIMEOUT, object);

        object = mappedConfig.get(API_CALL_ATTEMPT_TIMEOUT);
        if (object != null) apiCallAttemptTimeout = Config.loadIntegerValue(API_CALL_ATTEMPT_TIMEOUT, object);

        object = mappedConfig.get(LOG_TYPE);
        if (object != null) logType = (String) object;

        object = mappedConfig.get(METRICS_INJECTION);
        if(object != null) metricsInjection = Config.loadBooleanValue(METRICS_INJECTION, object);

        object = mappedConfig.get(METRICS_NAME);
        if(object != null ) metricsName = (String)object;

        object = mappedConfig.get(MAX_RETRIES);
        if (object != null) maxRetries = Config.loadIntegerValue(MAX_RETRIES, object);

        object = mappedConfig.get(MAX_CONCURRENCY);
        if (object != null) maxConcurrency = Config.loadIntegerValue(MAX_CONCURRENCY, object);

        object = mappedConfig.get(MAX_PENDING_CONNECTION_ACQUIRES);
        if (object != null) maxPendingConnectionAcquires = Config.loadIntegerValue(MAX_PENDING_CONNECTION_ACQUIRES, object);

        object = mappedConfig.get(CONNECTION_ACQUISITION_TIMEOUT);
        if (object != null) connectionAcquisitionTimeout = Config.loadIntegerValue(CONNECTION_ACQUISITION_TIMEOUT, object);
    }

    // --- Custom SetConfigMap Logic (Preserves complex string/map parsing) ---

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
                    // comma separated key:value pairs
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        if (keyValue.length == 2) {
                            functions.put(keyValue[0].trim(), keyValue[1].trim());
                        } else {
                            throw new ConfigException("functions entry must be in key:value format: " + pair);
                        }
                    }
                }
            } else if (object instanceof Map) {
                // If loaded as a map (standard YAML or JSON format), assign it
                functions = (Map<String, String>)object;
            } else {
                throw new ConfigException("functions must be a string string map.");
            }
        } else {
            // Initialize to empty map if not configured
            functions = Collections.emptyMap();
        }
    }
}
