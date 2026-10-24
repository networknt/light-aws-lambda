package com.networknt.aws.lambda;

import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;
import org.snakeyaml.engine.v2.schema.CoreSchema;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);
    private Map<String, Map<String, Object>> config;
    private static Configuration INSTANCE = new Configuration();

    public static Configuration getInstance() {
        return INSTANCE;
    }

    private Configuration() {
        load();
    }

    private void load() {
        LOGGER.warn("Loading app.yml with SnakeYAML Engine YAML 1.2 Core: legacy YAML 1.1 boolean, numeric, "
                + "and timestamp values may resolve differently. Review app.yml before rollout.");
        config = load(Configuration.class.getClassLoader().getResourceAsStream("app.yml"));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Map<String, Object>> load(InputStream input) {
        if (input == null) {
            throw new IllegalStateException("Classpath configuration app.yml was not found");
        }
        Load yaml = new Load(LoadSettings.builder()
                .setLabel("app.yml")
                .setSchema(new CoreSchema())
                .setAllowDuplicateKeys(true)
                .build());
        try (InputStream inputStream = input) {
            return (Map<String, Map<String, Object>>) yaml.loadFromInputStream(inputStream);
        } catch (YamlEngineException | ClassCastException e) {
            throw new IllegalStateException("Unable to parse app.yml using YAML 1.2 Core", e);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to close app.yml", e);
        }
    }

    public Map<String, Object> getStageConfig(String env) {
        return config.get(env);
    }
    public Map<String, Map<String, Object>> getConfig() { return config; }
}
