package com.networknt.aws.lambda.cache;

import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.utility.LambdaEnvVariables;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LambdaCache {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaCache.class);
    private static LambdaCache _internal;
    public static LambdaCache getInstance() {

        if (_internal == null) {
            _internal = new LambdaCache();
        }

        return _internal;
    }
    private final AmazonDynamoDB amazonDynamoDB;

    private LambdaCache() {
        this.amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withRegion(LambdaEnvVariables.AWS_REGION).build();
    }

    /**
     * Formats the applicationId into a dynamoDb table name.
     *
     * @param applicationId - app id.
     * @return - returns new string in dynamo db table name format.
     */
    public static String getDynamoDbTableName(String applicationId) {
        String dynamoDbTableName = applicationId;
        dynamoDbTableName = dynamoDbTableName.replace("-", "_");
        dynamoDbTableName = dynamoDbTableName.toUpperCase();
        return dynamoDbTableName;
    }

    /**
     * Gets a cached JWK from dynamoDb
     *
     * @param dynamoDbTableName - table name.
     * @param jwkKey - key the jwk would be found under.
     * @return - returns cached jwk and null if it was not found.
     */
    public CachedJwk getJwkFromCache(String dynamoDbTableName, String jwkKey) {
        var response = this.getFromCache(dynamoDbTableName, jwkKey);

        if (response == null || response.size() > 1) {
            LOG.error("Response is null or contains more than one entry in attribute: {}", response);
            throw new RuntimeException("Response is null or contains more than one entry in attribute: " + response);
        }

        if (response.size() == 0) {
            return null;
        }

        for (var entry : response.entrySet()) {
            var serialized = entry.getValue().getS();

            try {
                return new CachedJwk(jwkKey, serialized);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    /**
     * Pushes Jwk to dynamo db cache.
     *
     * @param dynamoDbTableName - table name.
     * @param jwkKey - attribute key.
     * @param jwk - attribute value.
     * @return - returns true if successful.
     */
    public boolean pushJwkToCache(String dynamoDbTableName, String jwkKey, JsonWebKey jwk) {
        CachedJwk cachedJwk;

        try {
            cachedJwk = new CachedJwk(jwkKey, jwk);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this.pushToCache(dynamoDbTableName, cachedJwk.getDynamoDbPayload());
    }

    /**
     * Gets cached middleware config from dynamo db.
     *
     * @param dynamoDbTableName - table name.
     * @param configKey - key the cached config is found under.
     * @return - returns cached config or null if nothing was found.
     * @param <T> - config class.
     */
    public <T> CachedConfig<T> getMiddlewareConfigFromCache(String dynamoDbTableName, String configKey) {
        var response = this.getFromCache(dynamoDbTableName, configKey);

        if (response == null || response.size() > 1) {
            LOG.error("Response is null or contains more than one entry in attribute: {}", response);
            throw new RuntimeException("Response is null or contains more than one entry in attribute: " + response);
        }

        if (response.size() == 0) {
            return null;
        }

        for (var entry : response.entrySet()) {
            var serialized = entry.getValue().getS();

            try {
                return new CachedConfig<T>(configKey, serialized);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    /**
     * Pushes config from a middleware handle to cache.
     *
     * @param dynamoDbTableName - name of the table.
     * @param configKey - key that we are placing the attribute value under.
     * @param config - config we want to cache.
     * @return - returns true if successfully cached the config.
     * @param <T> - config class.
     */
    public <T> boolean pushMiddlewareConfigToCache(String dynamoDbTableName, String configKey, T config) {
        CachedConfig<T> cachedConfig;

        try {
            cachedConfig = new CachedConfig<>(configKey, config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this.pushToCache(dynamoDbTableName, cachedConfig.getDynamoDbPayload());
    }

    /**
     * Pushes a JwtClaim to dynamodb.
     *
     * @param dynamoDbTableName - table name to push data to.
     * @param jwtKey - attribute key
     * @param claims - jwt claims
     * @return - return true if successful.
     */
    public boolean pushJwtToCache(String dynamoDbTableName, String jwtKey, JwtClaims claims) {
        CachedJwtClaims cachedJwt;
        try {
            cachedJwt = new CachedJwtClaims(jwtKey, claims);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this.pushToCache(dynamoDbTableName, cachedJwt.getDynamoDbPayload());
    }

    /**
     * Grabs the JwtClaim from dynamo db.
     * We use the jwtKey as the projection expression.
     *
     * @param dynamoDbTableName - dynamo db table name
     * @param jwtKey - attribute key used as a projection expression to get any existing keys found in the DB.
     * @return - returns JwtClaim and returns null if it does not exist.
     */
    public CachedJwtClaims getJwtFromCache(String dynamoDbTableName, String jwtKey) {
        var response = this.getFromCache(dynamoDbTableName, jwtKey);

        if (response == null || response.size() > 1) {
            LOG.error("Response is null or contains more than one entry in attribute: {}", response);
            throw new RuntimeException("Response is null or contains more than one entry in attribute: " + response);
        }

        if (response.size() == 0) {
            return null;
        }

        for (var entry : response.entrySet()) {

            var serialized = entry.getValue().getS();

            try {
                return new CachedJwtClaims(jwtKey, serialized);
            } catch (JsonProcessingException e) {
                LOG.error("Failed to convert string into JwtClaim class", e);
                throw new RuntimeException("Failed to convert string into JwtClaim class: " + e);
            }
        }

        return null;

    }

    /**
     * Pushes some payload to dynamodb.
     *
     * @param dynamoDbTableName - table name
     * @param cachePayload - hashmap of string attribute value.
     * @return - returns true if successful
     */
    private boolean pushToCache(String dynamoDbTableName, Map<String, AttributeValue> cachePayload) {
        var res = this.amazonDynamoDB.putItem(dynamoDbTableName, cachePayload);
        return this.isSuccessResponse(res.getSdkHttpMetadata());
    }

    /**
     * Gets some attribute from a specified table based on a projection expression.
     *
     * @param dynamoDbTableName - table name we are getting item from.
     * @param projectionExpression - projection expression that specifies the attributes.
     * @return - returns attribute map if item is found and returns null if no data was found.
     */
    private Map<String, AttributeValue> getFromCache(String dynamoDbTableName, String projectionExpression) {
        var getItemReq = new GetItemRequest();
        getItemReq.setTableName(dynamoDbTableName);
        getItemReq.setProjectionExpression(projectionExpression);

        var res = this.amazonDynamoDB.getItem(getItemReq);

        if (this.isSuccessResponse(res.getSdkHttpMetadata()))
            return res.getItem();

        else return null;
    }

    /**
     * Checks to see if response code is withing range of 200 - 299.
     *
     * @param res - res metadata from dynamo db request.
     * @return - return true if response code is successful.
     */
    private boolean isSuccessResponse(SdkHttpMetadata res) {
        return res.getHttpStatusCode() >= 200 && res.getHttpStatusCode() < 300;
    }


}
