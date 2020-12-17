package com.networknt.aws.lambda;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Configuration {
    private final Yaml yaml = new Yaml();
    private Map<String, Map<String, Object>> config;
    private static Configuration INSTANCE = new Configuration();

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private Configuration() {
        load();
    }

    private void load() {
        InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream("app.yml");
        config = yaml.load(inputStream);
    }

    public Map<String, Object> getStageConfig(String env) {
        return config.get(env);
    }
    public Map<String, Map<String, Object>> getConfig() { return config; }
}
