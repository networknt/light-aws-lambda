package com.networknt.aws.lambda.validator;

import com.networknt.jackson.JacksonDefaults;
import com.networknt.oas.OpenApiParser;
import com.networknt.oas.model.OpenApi3;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaIsolationTest {
    private static OpenApi3 api(String type) throws Exception {
        // Resolution base for the inline document; the parser does not read this file.
        var documentUrl = Path.of("schema-isolation.json").toUri().toURL();
        return (OpenApi3) new OpenApiParser().parse("""
                {"openapi":"3.0.3","info":{"title":"isolation","version":"1"},
                 "paths":{},"components":{"schemas":{"Value":{"type":"%s"}}}}
                """.formatted(type), documentUrl, false);
    }

    private static JsonNode reference() {
        return JacksonDefaults.mapper().readTree("{\"$ref\":\"#/components/schemas/Value\"}");
    }

    @Test
    void validationNeverAddsComponentsToTheCallerSchema() throws Exception {
        var validator = new SchemaValidator(api("integer"));
        var schema = reference();
        var original = schema.deepCopy();
        assertNull(validator.validate(JacksonDefaults.mapper().readTree("7"), schema, false, false));
        assertNotNull(validator.validate(JacksonDefaults.mapper().readTree("\"invalid\""), schema, false, false));
        assertEquals(original, schema);
        assertFalse(schema.has("components"));
    }

    @Test
    void callerOwnedComponentsRemainUnchanged() throws Exception {
        var schema = JacksonDefaults.mapper().readTree("""
                {"$ref":"#/components/schemas/Value",
                 "components":{"schemas":{"Value":{"type":"string"}}}}
                """);
        var original = schema.deepCopy();
        var validator = new SchemaValidator(api("integer"));
        // API components still take precedence for this validation, as before.
        assertNull(validator.validate(JacksonDefaults.mapper().readTree("7"), schema, false, false));
        assertEquals(original, schema);
    }

    @Test
    void concurrentValidationKeepsCallerInputsIsolated() throws Exception {
        var integerValidator = new SchemaValidator(api("integer"));
        var stringValidator = new SchemaValidator(api("string"));
        var schema = reference();
        var original = schema.deepCopy();
        var number = JacksonDefaults.mapper().readTree("7");
        var text = JacksonDefaults.mapper().readTree("\"7\"");
        var tasks = new ArrayList<Callable<Void>>();
        for (int i = 0; i < 40; i++) {
            tasks.add(() -> {
                assertNull(integerValidator.validate(number, schema, false, false));
                assertNotNull(stringValidator.validate(number, schema, false, false));
                assertNull(stringValidator.validate(text, schema, false, false));
                assertNotNull(integerValidator.validate(text, schema, false, false));
                assertNull(integerValidator.validate(text, schema, true, false));
                return null;
            });
        }
        var executor = Executors.newFixedThreadPool(4);
        try {
            for (var future : executor.invokeAll(tasks, 10, TimeUnit.SECONDS)) future.get(1, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
        }
        assertEquals(original, schema);
    }
}
