package com.networknt.aws.lambda.middleware.security;

import com.networknt.exception.ExpiredTokenException;
import com.networknt.http.client.ClientConfig;
import com.networknt.utility.FingerPrintUtil;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.keys.resolvers.X509VerificationKeyResolver;
import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.*;
import org.jose4j.jwx.JsonWebStructure;

import java.io.InputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.BiFunction;

public class JwtVerifier extends TokenVerifier {
    private static final Logger logger = LoggerFactory.getLogger(JwtVerifier.class);
    private static Map<String, JwtClaims> cache;
    private static Map<String, X509Certificate> certMap;
    private static List<JsonWebKey> jwkList;
    private static List<String> fingerPrints;

    public static final String JWT_CONFIG = "jwt";
    public static final String JWT_CERTIFICATE = "certificate";
    public static final String JWT_JWK = "jwk";
    public static final String JWT_CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    public static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    private static final String ENABLE_JWT_CACHE = "enableJwtCache";
    private static final String ENABLE_RELAXED_KEY_VALIDATION = "enableRelaxedKeyValidation";
    private static final int CACHE_EXPIRED_IN_MINUTES = 15;

    public static final String JWT_KEY_RESOLVER = "keyResolver";
    public static final String JWT_KEY_RESOLVER_X509CERT = "X509Certificate";
    public static final String JWT_KEY_RESOLVER_JWKS = "JsonWebKeySet";
    SecurityConfig config;
    Map<String, Object> jwtConfig;
    int secondsOfAllowedClockSkew;
    static Map<String, List<JsonWebKey>> jwksMap;


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
        switch ((String) jwtConfig.getOrDefault(JWT_KEY_RESOLVER, JWT_KEY_RESOLVER_X509CERT)) {
            case JWT_KEY_RESOLVER_JWKS:
                logger.debug("JWK resolver is enabled");
                break;
            case JWT_KEY_RESOLVER_X509CERT:
                logger.debug("X509 resolver is enabled");
                // load local public key certificates only if bootstrapFromKeyService is false
                certMap = new HashMap<>();
                fingerPrints = new ArrayList<>();
                if (jwtConfig.get(JWT_CERTIFICATE)!=null) {
                    Map<String, Object> keyMap = (Map<String, Object>) jwtConfig.get(JWT_CERTIFICATE);
                    logger.debug("keyMap = " + keyMap);
                    for(String kid: keyMap.keySet()) {
                        X509Certificate cert = null;
                        try {
                            cert = readCertificate((String)keyMap.get(kid));
                            logger.debug("cert = " + cert);
                        } catch (Exception e) {
                            logger.error("Exception:", e);
                        }
                        certMap.put(kid, cert);
                        fingerPrints.add(FingerPrintUtil.getCertFingerPrint(cert));
                    }
                }
                break;
            default:
                logger.info("{} not found or not recognized in jwt config. Use {} as default {}",
                        JWT_KEY_RESOLVER, JWT_KEY_RESOLVER_X509CERT, JWT_KEY_RESOLVER);
        }

    }

    /**
     * Read certificate from a file and convert it into X509Certificate object
     *
     * @param filename certificate file name
     * @return X509Certificate object
     * @throws Exception Exception while reading certificate
     */
    public X509Certificate readCertificate(String filename) {
        X509Certificate cert = null;
        try (InputStream inStream = JwtVerifier.class.getClassLoader().getResourceAsStream(filename)){
            if (inStream != null) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                cert = (X509Certificate) cf.generateCertificate(inStream);
                logger.debug("certificate is loaded " + cert);
            } else {
                logger.info("Certificate " + Encode.forJava(filename) + " not found.");
            }
        } catch (Exception e) {
            logger.error("Exception: ", e);
        }
        return cert;
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
     * Retrieve JWK set from the config file
     * @return List
     */
    private List<JsonWebKey> getJsonWebKeySetForToken(String filename) {
        try (InputStream inputStream = JwtVerifier.class.getClassLoader().getResourceAsStream(filename)) {
            if(inputStream != null) {
                String s = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
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
        ClientConfig clientConfig = ClientConfig.get();
        List<JsonWebKey> jwkList = jwksMap.get(kid);
        if (jwkList == null) {
            jwkList = getJsonWebKeySetForToken(kid);
            if (jwkList == null || jwkList.isEmpty()) {
                throw new RuntimeException("no JWK for kid: " + kid);
            }
            cacheJwkList(jwkList, null);
        }
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
