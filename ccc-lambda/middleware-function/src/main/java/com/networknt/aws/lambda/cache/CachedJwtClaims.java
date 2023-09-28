package com.networknt.aws.lambda.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jose4j.jwt.JwtClaims;

public class CachedJwtClaims extends CachedPayload {
    private final JwtClaims jwtClaims;

    public CachedJwtClaims(String jwtKey, String jwtClaimString) throws JsonProcessingException {
        super(jwtKey, jwtClaimString);
        this.jwtClaims = OBJECT_MAPPER.readValue(jwtClaimString, JwtClaims.class);
    }

    public CachedJwtClaims(String jwtKey, JwtClaims claims) throws JsonProcessingException {
        super(jwtKey, OBJECT_MAPPER.writeValueAsString(claims));
        this.jwtClaims = claims;
    }

    public JwtClaims getJwtClaims() {
        return jwtClaims;
    }
}
