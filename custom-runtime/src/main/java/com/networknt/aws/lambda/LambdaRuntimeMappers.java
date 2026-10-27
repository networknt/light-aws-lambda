package com.networknt.aws.lambda;

import com.networknt.jackson.JacksonDefaults;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared immutable mapper exclusively for AWS Lambda event payloads in the JVM and native runtimes.
 * Intentionally accepts unknown event properties, making it more permissive than the framework mapper.
 * Do not use it for configuration binding or validation.
 */
public final class LambdaRuntimeMappers {
    private static final ObjectMapper MAPPER = JacksonDefaults.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private LambdaRuntimeMappers() { }

    /** Returns the event-only mapper, accepting future AWS event properties. */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
