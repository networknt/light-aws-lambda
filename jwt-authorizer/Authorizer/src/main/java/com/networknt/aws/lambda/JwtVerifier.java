package com.networknt.aws.lambda;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.*;
import org.jose4j.jwx.JsonWebStructure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class JwtVerifier {
    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);
    private static Cache<String, JwtClaims> cache;
    private static final int CACHE_EXPIRED_IN_MINUTES = 15;
    private static final int JWT_CLOCK_SKEW_IN_SECONDS = 60;
    private static Map<String, List<JsonWebKey>> jwksMap;

    public JwtVerifier() {
        cache = Caffeine.newBuilder()
                // assuming that the clock screw time is less than 5 minutes
                .expireAfterWrite(CACHE_EXPIRED_IN_MINUTES, TimeUnit.MINUTES)
                .build();
    }

    /**
     * This method is to keep backward compatible for those call without VerificationKeyResolver.
     * @param jwt JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @return JwtClaims
     * @throws InvalidJwtException throw when the token is invalid
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry) throws InvalidJwtException, ExpiredTokenException {
        return verifyJwt(jwt, ignoreExpiry, this::getKeyResolver);
    }

    /**
     * Verify the jwt token and return the JwtClaims.
     *
     * @param jwt JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @return JwtClaims
     * @throws InvalidJwtException throw when the token is invalid
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, BiFunction<String, Boolean, VerificationKeyResolver> getKeyResolver) throws InvalidJwtException, ExpiredTokenException {
        JwtClaims claims = cache.getIfPresent(jwt);
        if(claims != null) {
            if(!ignoreExpiry) {
                try {
                    // if using our own client module, the jwt token should be renewed automatically
                    // and it will never expired here. However, we need to handle other clients.
                    if ((NumericDate.now().getValue() - JWT_CLOCK_SKEW_IN_SECONDS) >= claims.getExpirationTime().getValue())
                    {
                        logger.info("Cached jwt token is expired!");
                        throw new ExpiredTokenException("Token is expired");
                    }
                } catch (MalformedClaimException e) {
                    // This is cached token and it is impossible to have this exception
                    logger.error("MalformedClaimException:", e);
                }
            }
            // this claims object is signature verified already
            return claims;
        }
        JwtConsumer consumer = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification()
                .build();

        JwtContext jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        JsonWebStructure structure = jwtContext.getJoseObjects().get(0);
        // need this kid to load public key certificate for signature verification
        String kid = structure.getKeyIdHeaderValue();

        // so we do expiration check here manually as we have the claim already for kid
        // if ignoreExpiry is false, verify expiration of the token
        if(!ignoreExpiry) {
            try {
                if ((NumericDate.now().getValue() - JWT_CLOCK_SKEW_IN_SECONDS) >= claims.getExpirationTime().getValue())
                {
                    logger.info("jwt token is expired!");
                    throw new ExpiredTokenException("Token is expired");
                }
            } catch (MalformedClaimException e) {
                logger.error("MalformedClaimException:", e);
                throw new InvalidJwtException("MalformedClaimException", new ErrorCodeValidator.Error(ErrorCodes.MALFORMED_CLAIM, "Invalid ExpirationTime Format"), e, jwtContext);
            }
        }

        consumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(315360000) // use seconds of 10 years to skip expiration validation as we need skip it in some cases.
                .setSkipDefaultAudienceValidation()
                .setVerificationKeyResolver(getKeyResolver.apply(kid, true))
                .build();

        // Validate the JWT and process it to the Claims
        jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        cache.put(jwt, claims);
        return claims;
    }

    /**
     * Get VerificationKeyResolver based on the configuration settings
     * @return VerificationKeyResolver
     */
    private VerificationKeyResolver getKeyResolver(String kid, boolean isToken) {

        VerificationKeyResolver verificationKeyResolver = null;
        List<JsonWebKey> jwkList = jwksMap == null ? null : jwksMap.get(kid);
        if (jwkList == null) {
            jwkList = getJsonWebKeySetForToken();
            if (jwkList != null) {
                if (jwksMap == null) jwksMap = new HashMap<>();  // null if bootstrapFromKeyService is true
                jwksMap.put(kid, jwkList);
            }
        } else {
            logger.debug("Got Json web key set for kid: {} from local cache", kid);
        }
        if (jwkList != null) {
            verificationKeyResolver = new JwksVerificationKeyResolver(jwkList);
        }
        return verificationKeyResolver;
    }

    /**
     * Retrieve JWK set from oauth server with the given kid
     * @return List
     */
    private List<JsonWebKey> getJsonWebKeySetForToken() {
        try {
            String key = LambdaClient.getKey();
            logger.debug("Got Json Web Key {}", key);
            return new JsonWebKeySet(key).getJsonWebKeys();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new RuntimeException(e);
        }
    }

}
