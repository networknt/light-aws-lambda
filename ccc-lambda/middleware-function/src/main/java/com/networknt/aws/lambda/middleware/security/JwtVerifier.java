package com.networknt.aws.lambda.middleware.security;

import com.networknt.aws.lambda.cache.LambdaCache;
import com.networknt.aws.lambda.proxy.LambdaProxy;
import com.networknt.exception.ClientException;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.http.client.ClientConfig;
import com.networknt.http.client.oauth.OauthHelper;
import com.networknt.http.client.oauth.TokenKeyRequest;
import com.networknt.status.Status;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.*;
import org.jose4j.jwx.JsonWebStructure;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;

public class JwtVerifier extends TokenVerifier {
    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);
    static final String GET_KEY_ERROR = "ERR10066";
    private static Map<String, JwtClaims> cache;
    public static final String JWT_CONFIG = "jwt";
    public static final String JWT_JWK = "jwk";
    public static final String JWT_CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    public static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    private static final String ENABLE_JWT_CACHE = "enableJwtCache";
    private static final String ENABLE_RELAXED_KEY_VALIDATION = "enableRelaxedKeyValidation";
    private static final int CACHE_EXPIRED_IN_MINUTES = 15;

    SecurityConfig config;
    Map<String, Object> jwtConfig;
    int secondsOfAllowedClockSkew;
    static Map<String, List<JsonWebKey>> jwksMap;
    static String audience;  // this is the audience from the client.yml with single oauth provider.
    static Map<String, String> audienceMap; // this is the audience map from the client.yml with multiple oauth providers.


    public JwtVerifier(SecurityConfig config) {
        logger.debug("config = " + config);
        this.config = config;
        this.jwtConfig = config.getJwt();
        logger.debug("jwtConfig = " + jwtConfig);
        this.secondsOfAllowedClockSkew = (Integer)jwtConfig.get(JWT_CLOCK_SKEW_IN_SECONDS);

        if(Boolean.TRUE.equals(config.isEnableJwtCache())) {
            cache = new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(final Map.Entry eldest) {
                    return size() > 1000;
                }
            };
            logger.debug("jwt cache is enabled.");
        }

    }

    public void initJwkMap() {
        jwksMap = new HashMap<>();
        jwksMap = getJsonWebKeyMap();
    }

    /**
     * This method is to keep backward compatible for those call without VerificationKeyResolver. The single
     * auth server is used in this case.
     *
     * @param jwt          JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @param pathPrefix   pathPrefix used to cache the jwt token
     * @param requestPath  request path
     * @param jwkServiceIds A list of serviceIds from the UnifiedSecurityHandler
     * @return JwtClaims
     * @throws InvalidJwtException   throw when the token is invalid
     * @throws com.networknt.exception.ExpiredTokenException throw when the token is expired
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, String pathPrefix, String requestPath, List<String> jwkServiceIds) throws InvalidJwtException, ExpiredTokenException {
        return verifyJwt(jwt, ignoreExpiry, pathPrefix, requestPath, jwkServiceIds, this::getKeyResolver);
    }

    /**
     * This method is to keep backward compatible for those call without VerificationKeyResolver.
     * @param jwt JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @return JwtClaims
     * @throws InvalidJwtException throw when the token is invalid
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry) throws InvalidJwtException, ExpiredTokenException {
        return verifyJwt(jwt, ignoreExpiry, null, null, null, this::getKeyResolver);
    }

    /**
     * Verify the jwt token and return the JwtClaims.
     *
     * @param jwt JWT token
     * @param ignoreExpiry indicate if the expiry will be ignored
     * @return JwtClaims
     * @throws InvalidJwtException throw when the token is invalid
     */
    public JwtClaims verifyJwt(String jwt, boolean ignoreExpiry, String pathPrefix, String requestPath, List<String> jwkServiceIds, BiFunction<String, Boolean, VerificationKeyResolver> getKeyResolver) throws InvalidJwtException, ExpiredTokenException {
        JwtClaims claims;
        if (Boolean.TRUE.equals(config.isEnableJwtCache())) {

            if(pathPrefix != null) {
                claims = cache.get(pathPrefix + ":" + jwt);
            } else {
                claims = cache.get(jwt);
            }

            if (claims != null) {
                checkExpiry(ignoreExpiry, claims, secondsOfAllowedClockSkew, null);
                // this claims object is signature verified already
                return claims;
            }
        }

        JwtConsumer consumer;
        JwtConsumerBuilder pKeyBuilder = new JwtConsumerBuilder()
                .setSkipAllValidators()
                .setDisableRequireSignature()
                .setSkipSignatureVerification();

        if (config.isEnableRelaxedKeyValidation()) {
            pKeyBuilder.setRelaxVerificationKeyValidation();
        }

        consumer = pKeyBuilder.build();

        JwtContext jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        JsonWebStructure structure = jwtContext.getJoseObjects().get(0);
        // need this kid to load public key certificate for signature verification
        String kid = structure.getKeyIdHeaderValue();

        // so we do expiration check here manually as we have the claim already for kid
        // if ignoreExpiry is false, verify expiration of the token
        checkExpiry(ignoreExpiry, claims, secondsOfAllowedClockSkew, jwtContext);

        // validate the audience against the configured audience.
        // validateAudience(claims, requestPath, jwkServiceIds, jwtContext);

        JwtConsumerBuilder jwtBuilder = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setAllowedClockSkewInSeconds(315360000) // use seconds of 10 years to skip expiration validation as we need skip it in some cases.
                .setSkipDefaultAudienceValidation()
                .setVerificationKeyResolver(getKeyResolver.apply(kid, true));

        if (config.isEnableRelaxedKeyValidation()) {
            jwtBuilder.setRelaxVerificationKeyValidation();
        }

        consumer = jwtBuilder.build();

        // Validate the JWT and process it to the Claims
        jwtContext = consumer.process(jwt);
        claims = jwtContext.getJwtClaims();
        if (Boolean.TRUE.equals(config.isEnableJwtCache())) {
            if(pathPrefix != null) {
                cache.put(pathPrefix + ":" + jwt, claims);
            } else {
                cache.put(jwt, claims);
            }

            if(cache.size() > config.getJwtCacheFullSize()) {
                logger.warn("JWT cache exceeds the size limit " + config.getJwtCacheFullSize());
            }
        }
        return claims;
    }

    /**
     * Retrieve JWK set from all possible oauth servers. If there are multiple servers in the client.yml, get all
     * the jwk by iterate all of them. In case we have multiple jwks, the cache will have a prefix so that verify
     * action won't cross fired.
     *
     * @return {@link Map} of {@link List}
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<JsonWebKey>> getJsonWebKeyMap() {
        // the jwk indicator will ensure that the kid is not concat to the uri for path parameter.
        // the kid is not needed to get JWK. We need to figure out only one jwk server or multiple.
        //jwksMap = new HashMap<>();
        ClientConfig clientConfig = ClientConfig.load();
        Map<String, Object> tokenConfig = clientConfig.getTokenConfig();
        Map<String, Object> keyConfig = (Map<String, Object>) tokenConfig.get(ClientConfig.KEY);
        if (clientConfig.isMultipleAuthServers()) {
            // iterate all the configured auth server to get JWK.
            Map<String, Object> serviceIdAuthServers = (Map<String, Object>) keyConfig.get(ClientConfig.SERVICE_ID_AUTH_SERVERS);
            if (serviceIdAuthServers != null && serviceIdAuthServers.size() > 0) {
                audienceMap = new HashMap<>();
                for (Map.Entry<String, Object> entry : serviceIdAuthServers.entrySet()) {
                    String serviceId = entry.getKey();
                    Map<String, Object> authServerConfig = (Map<String, Object>) entry.getValue();
                    // based on the configuration, we can identify if the entry is for jwk retrieval for jwt or swt introspection. For jwk,
                    // there is no clientId and clientSecret. For token introspection, clientId and clientSecret is in the config.
                    if(authServerConfig.get(ClientConfig.CLIENT_ID) != null && authServerConfig.get(ClientConfig.CLIENT_SECRET) != null) {
                        // this is the entry for swt introspection, skip here.
                        continue;
                    }
                    // construct audience map for audience validation.
                    String audience = (String) authServerConfig.get(ClientConfig.AUDIENCE);
                    if (audience != null) {
                        if (logger.isTraceEnabled()) logger.trace("audience {} is mapped to serviceId {}", audience, serviceId);
                        audienceMap.put(serviceId, audience);
                    }
                    // get the jwk from the auth server.
                    TokenKeyRequest keyRequest = new TokenKeyRequest(null, true, authServerConfig);
                    try {

                        if (logger.isDebugEnabled())
                            logger.debug("Getting Json Web Key list from {} for serviceId {}", keyRequest.getServerUrl(), entry.getKey());

                        String key = OauthHelper.getKey(keyRequest);

                        if (logger.isDebugEnabled())
                            logger.debug("Got Json Web Key = " + key);

                        List<JsonWebKey> jwkList = new JsonWebKeySet(key).getJsonWebKeys();

                        if (jwkList == null || jwkList.isEmpty()) {
                            if (logger.isErrorEnabled())
                                logger.error("Cannot get JWK from OAuth server.");

                        } else {

                            // TODO - combine this into common cache class
                            if (LambdaProxy.CONFIG.isEnableDynamoDbCache()) {
                                LambdaCache.getInstance().setJwk(LambdaProxy.CONFIG.getLambdaAppId(), key);
                            } else {
                                for (JsonWebKey jwk : jwkList) {

                                    jwksMap.put(serviceId + ":" + jwk.getKeyId(), jwkList);
                                    if (logger.isDebugEnabled())
                                        logger.debug("Successfully cached JWK for serviceId {} kid {} with key {}", serviceId, jwk.getKeyId(), serviceId + ":" + jwk.getKeyId());

                                }
                            }


                        }
                    } catch (JoseException ce) {
                        if (logger.isErrorEnabled())
                            logger.error("Failed to get JWK set. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);

                    } catch (ClientException ce) {

                        if (logger.isErrorEnabled())
                            logger.error("Failed to get key. - {} - {} ", new Status(GET_KEY_ERROR), ce.getMessage(), ce);
                    }
                }
            } else {
                // log an error as there is no service entry for the jwk retrieval.
                logger.error("serviceIdAuthServers property is missing or empty in the token key configuration");
            }
        } else {

            // TODO - combine this into common cache class
            if (LambdaProxy.CONFIG.isEnableDynamoDbCache()) {
                String cachedKey = LambdaCache.getInstance().getJwk(LambdaProxy.CONFIG.getLambdaAppId());
                if (cachedKey != null) {
                    List<JsonWebKey> set = null;
                    try {
                        set = new JsonWebKeySet(cachedKey).getJsonWebKeys();
                    } catch (JoseException e) {
                        throw new RuntimeException(e);
                    }
                    Map<String, List<JsonWebKey>> map = new HashMap<>();
                    for (var jwk : set) {
                        map.put(jwk.getKeyId(), set);
                    }
                    return map;
                }
            }

            // get audience from the key config
            audience = (String) keyConfig.get(ClientConfig.AUDIENCE);
            if(logger.isTraceEnabled()) logger.trace("A single audience {} is configured in client.yml", audience);
            // there is only one jwk server.
            TokenKeyRequest keyRequest = new TokenKeyRequest(null, true, null);
            try {
                if (logger.isDebugEnabled())
                    logger.debug("Getting Json Web Key list from {}", keyRequest.getServerUrl());

                String key = OauthHelper.getKey(keyRequest);

                if (logger.isDebugEnabled())
                    logger.debug("Got Json Web Key = " + key);

                List<JsonWebKey> jwkList = new JsonWebKeySet(key).getJsonWebKeys();
                if (jwkList == null || jwkList.isEmpty()) {
                    throw new RuntimeException("cannot get JWK from OAuth server");
                }

                // TODO - combine this into common cache class
                logger.debug("setting jwk list: {}", jwkList);
                if (LambdaProxy.CONFIG.isEnableDynamoDbCache()) {
                    LambdaCache.getInstance().setJwk(LambdaProxy.CONFIG.getLambdaAppId(), key);
                } else {
                    for (JsonWebKey jwk : jwkList) {
                        jwksMap.put(jwk.getKeyId(), jwkList);

                        if (logger.isDebugEnabled())
                            logger.debug("Successfully cached JWK for kid {}", jwk.getKeyId());
                    }
                }


            } catch (JoseException ce) {

                if (logger.isErrorEnabled())
                    logger.error("Failed to get JWK. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);

            } catch (ClientException ce) {

                if (logger.isErrorEnabled())
                    logger.error("Failed to get Key. - {} - {}", new Status(GET_KEY_ERROR), ce.getMessage(), ce);
            }
        }

        // TODO - combine this into common cache class
        if (LambdaProxy.CONFIG.isEnableDynamoDbCache()) {
            var webKey = LambdaCache.getInstance().getJwk(LambdaProxy.CONFIG.getLambdaAppId());
            List<JsonWebKey> set = null;
            try {
                set = new JsonWebKeySet(webKey).getJsonWebKeys();
            } catch (JoseException e) {
                throw new RuntimeException(e);
            }
            Map<String, List<JsonWebKey>> map = new HashMap<>();
            for (var jwk : set) {
                map.put(jwk.getKeyId(), set);
            }
            return map;
        } else {
            return jwksMap;
        }


    }

    /**
     * Retrieve JWK set from the config file
     * @return List
     */
    private List<JsonWebKey> getJsonWebKeySetForToken(String filename) {
        try (InputStream inputStream = JwtVerifier.class.getClassLoader().getResourceAsStream(filename)) {
            if(inputStream != null) {
                String s = new Scanner(inputStream, StandardCharsets.UTF_8).useDelimiter("\\A").next();
                if(logger.isTraceEnabled()) logger.trace("Got Json Web Key {}", s);
                return new JsonWebKeySet(s).getJsonWebKeys();
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
            return null;
        }
    }

    /**
     * Checks expiry of a jwt token from the claim.
     *
     * @param ignoreExpiry     - flag set if we want to ignore expired tokens or not.
     * @param claim            - jwt claims
     * @param allowedClockSkew - seconds of allowed skew in token expiry
     * @param context          - jwt context
     * @throws com.networknt.exception.ExpiredTokenException - thrown when token is expired
     * @throws InvalidJwtException   - thrown when the token is malformed/invalid
     */
    private static void checkExpiry(boolean ignoreExpiry, JwtClaims claim, int allowedClockSkew, JwtContext context) throws com.networknt.exception.ExpiredTokenException, InvalidJwtException {
        if (!ignoreExpiry) {
            try {
                // if using our own client module, the jwt token should be renewed automatically
                // and it will never expire here. However, we need to handle other clients.
                if ((NumericDate.now().getValue() - allowedClockSkew) >= claim.getExpirationTime().getValue()) {
                    logger.info("Cached jwt token is expired!");
                    throw new com.networknt.exception.ExpiredTokenException("Token is expired");
                }
            } catch (MalformedClaimException e) {
                // This is cached token and it is impossible to have this exception
                logger.error("MalformedClaimException:", e);
                throw new InvalidJwtException("MalformedClaimException", new ErrorCodeValidator.Error(ErrorCodes.MALFORMED_CLAIM, "Invalid ExpirationTime Format"), e, context);
            }
        }
    }

    /**
     * Get VerificationKeyResolver based on the kid and isToken indicator. For the implementation, we check
     * the jwk first and 509Certificate if the jwk cannot find the kid. Basically, we want to iterate all
     * the resolvers and find the right one with the kid.
     *
     * @param kid key id from the JWT token
     * @return VerificationKeyResolver
     */
    private VerificationKeyResolver getKeyResolver(String kid, boolean isToken) {
        // jwk is always used here.
        List<JsonWebKey> jwkList;
        if (LambdaProxy.CONFIG.isEnableDynamoDbCache()) {
            String key = LambdaCache.getInstance().getJwk(LambdaProxy.CONFIG.getLambdaAppId());
            try {
                jwkList = new JsonWebKeySet(key).getJsonWebKeys();
            } catch (JoseException e) {
                throw new RuntimeException(e);
            }
            if (jwkList == null) {
                jwkList = getJsonWebKeySetForToken(kid);
                if (jwkList == null || jwkList.isEmpty()) {
                    throw new RuntimeException("no JWK for kid: " + kid);
                }
                LambdaCache.getInstance().setJwk(LambdaProxy.CONFIG.getLambdaAppId(), key);
            }
        } else {
            jwkList = jwksMap.get(kid);
            if (jwkList == null) {
                jwkList = getJsonWebKeySetForToken(kid);
                if (jwkList == null || jwkList.isEmpty()) {
                    throw new RuntimeException("no JWK for kid: " + kid);
                }
                cacheJwkList(jwkList, null);
            }
        }

        logger.debug("getKeyResolver: kid --> {}", kid);



        logger.debug("Got Json web key set from local cache");
        return new JwksVerificationKeyResolver(jwkList);
    }

    private void cacheJwkList(List<JsonWebKey> jwkList, String serviceId) {
        for (JsonWebKey jwk : jwkList) {
            if(serviceId != null) {
                if(logger.isTraceEnabled()) logger.trace("cache the jwkList with serviceId {} kid {} and key {}", serviceId, jwk.getKeyId(), serviceId + ":" + jwk.getKeyId());
                jwksMap.put(serviceId + ":" + jwk.getKeyId(), jwkList);
            } else {
                if(logger.isTraceEnabled()) logger.trace("cache the jwkList with kid and only kid as key", jwk.getKeyId());
                jwksMap.put(jwk.getKeyId(), jwkList);
            }
        }
    }

}
