package com.networknt.aws.lambda;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.jsonoverlay.Overlay;
import com.networknt.oas.model.OpenApi3;
import com.networknt.oas.model.impl.OpenApi3Impl;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationMessage;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Validate a value against the schema defined in an OpenAPI specification.
 * <p>
 * Supports validation of properties and request/response bodies, and supports schema references.
 *
 * @author Steve Hu
 */
public class SchemaValidator {
    private static final String COMPONENTS_FIELD = "components";
    static ObjectMapper objectMapper = new ObjectMapper();
    private final OpenApi3 api;
    private JsonNode jsonNode;
    private final SchemaValidatorsConfig defaultConfig;

    /**
     * Build a new validator with no API specification.
     * <p>
     * This will not perform any validation of $ref references that reference local schemas.
     *
     */
    public SchemaValidator() {
        this(null);
    }

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for. If provided, is used to retrieve schemas in components
     *            for use in references.
     */
    public SchemaValidator(final OpenApi3 api) {
        this.api = api;
        this.jsonNode = Overlay.toJson((OpenApi3Impl)api).get("components");
        this.defaultConfig = new SchemaValidatorsConfig();
        this.defaultConfig.setTypeLoose(true);
    }

    /**
     * Validate the given value against the given property schema.
     *
     * @param value The value to validate
     * @param schema The property schema to validate the value against
     *
     * @return A status containing error code and description
     */
    public ValidationMessage validate(final Object value, final JsonNode schema) {
        return doValidate(value, schema, "$");
    }

    /**
     * Validate the given value against the given property schema.
     *
     * @param value The value to validate
     * @param schema The property schema to validate the value against
     * @param at The name of the property being validated
     * @return Status object
     */
    public ValidationMessage validate(final Object value, final JsonNode schema, String at) {
        return doValidate(value, schema, at);
    }

    private ValidationMessage doValidate(final Object value, final JsonNode schema, String at) {
        requireNonNull(schema, "A schema is required");

        Set<ValidationMessage> processingReport = null;
        try {
            if(jsonNode != null) {
                ((ObjectNode)schema).set(COMPONENTS_FIELD, jsonNode);
            }
            JsonSchema jsonSchema = JsonSchemaFactory.getInstance().getSchema(schema, defaultConfig);
            final JsonNode content = objectMapper.valueToTree(value);
            processingReport = jsonSchema.validate(content, content, at);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ValidationMessage vm = null;
        if(processingReport != null && processingReport.size() > 0) {
            vm = processingReport.iterator().next();
        }
        return vm;
    }
}
