package com.networknt.aws.lambda;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class Configuration {
    private final Yaml yaml = new Yaml();
    private Map<String, Map<String, Object>> configuration;

    public Configuration() {
        load();
    }

    private void load() {
        InputStream inputStream = Configuration.class.getResourceAsStream("/app.yml");
        configuration = yaml.load(inputStream);
    }

    public Map<String, Object> getConfigMap(String env) {
        return configuration.get(env);
    }
}
