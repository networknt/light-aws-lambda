package com.networknt.aws.lambda;

import org.junit.jupiter.api.Test;
import org.snakeyaml.engine.v2.exceptions.YamlEngineException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationFailureTest {
    @Test
    void missingResourceNamesAppYaml() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> Configuration.load(null));
        assertEquals("Classpath configuration app.yml was not found", failure.getMessage());
    }

    @Test
    void malformedYamlRetainsParserCauseAndClosesStream() {
        assertInvalidYaml("Prod: [unfinished");
    }

    @Test
    void legacyTimestampTagHasClearDiagnostic() {
        assertInvalidYaml("Prod:\n  date: !!timestamp 2026-10-24\n");
    }

    @Test
    void tabIndentationHasClearDiagnostic() {
        assertInvalidYaml("Prod:\n\tvalue: invalid\n");
    }

    @Test
    void successfulLoadClosesStream() {
        TrackingStream input = new TrackingStream("Prod:\n  value: valid\n");
        assertEquals("valid", Configuration.load(input).get("Prod").get("value"));
        assertTrue(input.closed);
    }

    @Test
    void closeFailureRetainsIoCause() {
        IOException cause = new IOException("synthetic close failure");
        ByteArrayInputStream input = new ByteArrayInputStream("Prod: {}".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                throw cause;
            }
        };
        UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> Configuration.load(input));
        assertEquals("Unable to close app.yml", failure.getMessage());
        assertSame(cause, failure.getCause());
    }

    private void assertInvalidYaml(String yaml) {
        TrackingStream input = new TrackingStream(yaml);
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> Configuration.load(input));
        assertEquals("Unable to parse app.yml using YAML 1.2 Core", failure.getMessage());
        assertInstanceOf(YamlEngineException.class, failure.getCause());
        assertTrue(input.closed);
    }

    private static class TrackingStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackingStream(String yaml) {
            super(yaml.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
