package com.networknt.aws.lambda;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.schema.CoreSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

public class Configuration {
    private Map<String, Map<String, Object>> config;
    private static Configuration INSTANCE = new Configuration();

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private Configuration() {
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Load yaml = new Load(LoadSettings.builder()
                .setSchema(new CoreSchema())
                .setAllowDuplicateKeys(true)
                .build());
        try (InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("app.yml")) {
            config = (Map<String, Map<String, Object>>) yaml.loadFromInputStream(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to close app.yml", e);
        }
    }

    public Map<String, Object> getStageConfig(String env) {
        return config.get(env);
    }
    public Map<String, Map<String, Object>> getConfig() { return config; }
}
