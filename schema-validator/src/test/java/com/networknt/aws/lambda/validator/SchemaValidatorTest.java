package com.networknt.aws.lambda.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.networknt.status.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SchemaValidatorTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void shouldConstructWithoutAnApi() {
        new SchemaValidator();
    }

    @Test
    void shouldKeepLooseAndStrictTypeProfilesSeparate() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        JsonNode schema = MAPPER.readTree("{\"type\":\"integer\"}");

        assertNull(validator.validate(new TextNode("42"), schema, true, false));
        assertNotNull(validator.validate(new TextNode("42"), schema, false, false));
    }

    @Test
    void shouldHonorNullableKeywordProfile() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        JsonNode schema = MAPPER.readTree("{\"type\":\"string\",\"nullable\":true}");

        assertNull(validator.validate(NullNode.getInstance(), schema, false, true));
        assertNotNull(validator.validate(NullNode.getInstance(), schema, false, false));
    }

    @Test
    void shouldPreserveJsonPointerLocationFormat() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        JsonNode schema = MAPPER.readTree("{\"type\":\"object\",\"properties\":{\"age\":{\"type\":\"integer\"}}}");
        JsonNode value = MAPPER.readTree("{\"age\":\"invalid\"}");

        Status status = validator.validate(value, schema, "body");

        assertNotNull(status);
        assertTrue(status.getDescription().contains("/body/age:"), status.getDescription());
    }

    @Test
    void shouldAllowStandardNonDefaultDialects() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        JsonNode schema = MAPPER.readTree("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"integer\"}");

        Status status = validator.validate(new TextNode("invalid"), schema, false, false);

        assertNotNull(status);
        assertEquals(SchemaValidator.VALIDATOR_SCHEMA, status.getCode());
    }

    @Test
    void shouldFailClosedWhenSchemaCannotBeBuilt() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        JsonNode schema = MAPPER.readTree("{\"type\":\"string\",\"pattern\":\"[\"}");

        Status status = validator.validate(new TextNode("value"), schema, false, false);

        assertNotNull(status);
        assertEquals(SchemaValidator.VALIDATOR_SCHEMA_INVALID_JSON, status.getCode());
    }

    @Test
    void shouldKeepCustomMessageKeywordDisabled() throws Exception {
        SchemaValidator validator = new SchemaValidator();
        JsonNode schema = MAPPER.readTree("{\"type\":\"object\",\"required\":[\"name\"],\"message\":{\"required\":\"A custom validation message\"}}");

        Status status = validator.validate(MAPPER.createObjectNode(), schema, false, false);

        assertNotNull(status);
        assertFalse(status.getDescription().contains("A custom validation message"), status.getDescription());
    }
}
