package com.networknt.aws.lambda;

import com.networknt.jackson.JacksonDefaults;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;

/** Shared immutable event mapper for the JVM and native Lambda runtimes. */
public final class LambdaRuntimeMappers {
    private static final ObjectMapper MAPPER = JacksonDefaults.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private LambdaRuntimeMappers() { }

    /** Keeps the framework defaults while accepting future AWS event properties. */
    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
