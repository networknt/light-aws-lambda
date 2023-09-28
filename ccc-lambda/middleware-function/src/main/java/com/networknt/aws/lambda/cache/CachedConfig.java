package com.networknt.aws.lambda.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

public class CachedConfig<T> extends CachedPayload {

    private final T config;

    public CachedConfig(String configKey, String configString) throws JsonProcessingException {
        super(configKey, configString);
        this.config = OBJECT_MAPPER.readValue(configString, new TypeReference<T>() {});
    }

    public CachedConfig(String configKey, T config) throws JsonProcessingException {
        super(configKey, OBJECT_MAPPER.writeValueAsString(config));
        this.config = config;
    }

    public T getConfig() {
        return config;
    }
}
