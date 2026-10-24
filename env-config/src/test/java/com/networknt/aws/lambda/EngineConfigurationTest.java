package com.networknt.aws.lambda;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineConfigurationTest {
    @Test
    void loadsStageConfigurationFromClasspath() {
        Configuration configuration = Configuration.getInstance();
        assertEquals("https://prod.example.com/oauth2/aus41bwp2BafABp7h1d6/v1/keys",
                configuration.getStageConfig("Prod").get("jwkUrl"));
        assertSame(configuration.getConfig().get("Prod"), configuration.getStageConfig("Prod"));
        assertNull(configuration.getStageConfig("Missing"));
    }

    @Test
    void usesYaml12CoreScalarResolution() {
        Map<String, Object> config = Configuration.getInstance().getStageConfig("Engine");
        assertEquals(true, config.get("enabled"));
        assertEquals("on", config.get("legacyBoolean"));
        assertEquals(755, config.get("decimal"));
        assertEquals(493, config.get("octal"));
        assertEquals("0b11", config.get("binary"));
        assertEquals("1_000", config.get("separatedInteger"));
        assertEquals("1:30", config.get("sexagesimal"));
        assertEquals("1:30.5", config.get("sexagesimalFloat"));
        assertEquals("1_0.5", config.get("separatedFloat"));
        assertEquals("2026-10-24", config.get("timestamp"));
        assertTrue(config.containsKey("empty"));
        assertNull(config.get("empty"));
    }

    @Test
    void preservesCollectionsAliasesAndUnicode() {
        Map<String, Object> config = Configuration.getInstance().getStageConfig("Engine");
        assertEquals(List.of("https://one.example.com", "https://two.example.com"), config.get("endpoints"));
        assertEquals(config.get("endpoints"), config.get("copiedEndpoints"));
        assertEquals("Unicode: café λ", config.get("message"));
    }

    @Test
    void preservesLastValueForDuplicateKeys() {
        assertEquals("last", Configuration.getInstance().getStageConfig("Engine").get("duplicate"));
    }
}
