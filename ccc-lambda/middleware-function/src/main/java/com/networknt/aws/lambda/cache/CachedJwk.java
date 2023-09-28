package com.networknt.aws.lambda.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jose4j.jwk.JsonWebKey;

public class CachedJwk extends CachedPayload {

    private final JsonWebKey jwk;

    public CachedJwk(String jwkKey, String jwk) throws JsonProcessingException {
        super(jwkKey, jwk);
        this.jwk = OBJECT_MAPPER.readValue(jwk, JsonWebKey.class);
    }

    public CachedJwk(String jwkKey, JsonWebKey jwk) throws JsonProcessingException {
        super(jwkKey, OBJECT_MAPPER.writeValueAsString(jwk));
        this.jwk = jwk;
    }

    public JsonWebKey getJwk() {
        return jwk;
    }
}
