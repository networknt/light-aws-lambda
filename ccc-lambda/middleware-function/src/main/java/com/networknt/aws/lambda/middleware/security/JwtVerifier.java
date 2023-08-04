package com.networknt.aws.lambda.middleware.security;

import com.networknt.aws.lambda.Configuration;
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

public class JwtVerifier {
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
    private static final int CACHE_EXPIRED_IN_MINUTES = 15;

    public static final String JWT_KEY_RESOLVER = "keyResolver";
    public static final String JWT_KEY_RESOLVER_X509CERT = "X509Certificate";
    public static final String JWT_KEY_RESOLVER_JWKS = "JsonWebKeySet";

    private String stage;
    Map<String, Map<String, Object>> config = Configuration.getInstance().getConfig();
    Map<String, Object> stageConfig;
    Map<String, Object> jwtConfig;
    int secondsOfAllowedClockSkew;
    Boolean enableJwtCache;

    public JwtVerifier(String stage) {
        this.stage = stage;
        stageConfig = config.get(stage);
        logger.debug("stage = " + stage + " stageConfig = " + stageConfig);
        jwtConfig = (Map<String, Object>)stageConfig.get(JWT_CONFIG);
        logger.debug("jwtConfig = " + jwtConfig);
        this.secondsOfAllowedClockSkew = (Integer)jwtConfig.get(JWT_CLOCK_SKEW_IN_SECONDS);
        this.enableJwtCache = (Boolean)stageConfig.get(ENABLE_JWT_CACHE);
        if(Boolean.TRUE.equals(enableJwtCache)) {
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
        JwtClaims claims = cache.get(jwt);
        if(claims != null) {
            logger.debug("There is a cache claims for the JWT");
            if(!ignoreExpiry) {
                try {
                    // if using our own client module, the jwt token should be renewed automatically
                    // and it will never expired here. However, we need to handle other clients.
                    if ((NumericDate.now().getValue() - secondsOfAllowedClockSkew) >= claims.getExpirationTime().getValue())
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
        logger.debug("get the kid = " + kid);
        // so we do expiration check here manually as we have the claim already for kid
        // if ignoreExpiry is false, verify expiration of the token
        if(!ignoreExpiry) {
            try {
                if ((NumericDate.now().getValue() - secondsOfAllowedClockSkew) >= claims.getExpirationTime().getValue())
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
        logger.debug("processed jwt with the claims " + claims);
        cache.put(jwt, claims);
        return claims;
    }

    /**
     * Get VerificationKeyResolver based on the configuration settings
     * @return VerificationKeyResolver
     */
    private VerificationKeyResolver getKeyResolver(String kid, boolean isToken) {
        VerificationKeyResolver verificationKeyResolver = null;
        String keyResolver = (String)jwtConfig.get(JWT_KEY_RESOLVER);
        logger.debug("keyResolver = " + keyResolver);
        switch (keyResolver) {
            default:
            case JWT_KEY_RESOLVER_X509CERT:
                // get the public key certificate from the cache that is loaded from security.yml if it is not there,
                // go to OAuth2 server /oauth2/key endpoint to get the public key certificate with kid as parameter.
                X509Certificate certificate = certMap == null ? null : certMap.get(kid);
                X509VerificationKeyResolver x509VerificationKeyResolver = new X509VerificationKeyResolver(certificate);
                x509VerificationKeyResolver.setTryAllOnNoThumbHeader(true);
                verificationKeyResolver = x509VerificationKeyResolver;
                break;

            case JWT_KEY_RESOLVER_JWKS:
                if(jwkList == null) {
                    String jwkName = (String)jwtConfig.get(JWT_JWK);
                    jwkList = getJsonWebKeySetForToken(jwkName);
                }
                verificationKeyResolver = new JwksVerificationKeyResolver(jwkList);
                break;
        }
        return verificationKeyResolver;
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

}
