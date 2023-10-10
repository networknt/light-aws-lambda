package com.networknt.aws.lambda.cache;

import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.utility.LambdaEnvVariables;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwt.JwtClaims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LambdaCache {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaCache.class);
    private static final String DYNAMO_DB_TABLE_NAME = "LAMBDA_NATIVE_PROXY";
    private static LambdaCache _internal;
    private static final int TABLE_LIST_LIMIT = 100;
    public static LambdaCache getInstance() {

        if (_internal == null) {
            _internal = new LambdaCache();
        }

        return _internal;
    }
    private final AmazonDynamoDB dynamoClient;
    private final DynamoDB dynamoDB;
    boolean tableInitiated;

    private LambdaCache() {
        this.tableInitiated = false;
        this.dynamoClient = AmazonDynamoDBClientBuilder
                .standard()
                .withRegion(System.getenv(LambdaEnvVariables.AWS_REGION))
                .build();

        this.dynamoDB = new DynamoDB(dynamoClient);
    }

    /**
     * Formats the applicationId into a dynamoDb table name.
     *
     * @param applicationId - app id.
     * @return - returns new string in dynamo db table name format.
     */
    public static String getDynamoDbTableName(String applicationId) {

        LOG.debug("Getting table name from appId '{}'", applicationId);

        var dynamoDbTableName = applicationId;
        dynamoDbTableName = dynamoDbTableName.replace("-", "_");
        dynamoDbTableName = dynamoDbTableName.toUpperCase();

        LOG.debug("DynamoDbTable name is '{}'", dynamoDbTableName);

        return dynamoDbTableName;
    }

    /**
     * Gets a cached JWK from dynamoDb
     *
     * @param dynamoDbTableName - table name.
     * @param jwkKey            - key the jwk would be found under.
     * @return - returns cached jwk and null if it was not found.
     */
    public CachedJwk getJwkFromCache(String dynamoDbTableName, String jwkKey) {
        var response = this.getFromCache(jwkKey);

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
     * @param jwkKey            - attribute key.
     * @param jwk               - attribute value.
     * @return - returns true if successful.
     */
    public boolean pushJwkToCache(String dynamoDbTableName, String jwkKey, JsonWebKey jwk) {
        CachedJwk cachedJwk;

        try {
            cachedJwk = new CachedJwk(jwkKey, jwk);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this.pushToCache(cachedJwk.getDynamoDbPayload());
    }

    /**
     * Gets cached middleware config from dynamo db.
     *
     * @param configKey         - key the cached config is found under.
     * @param <T>               - config class.
     * @return - returns cached config or null if nothing was found.
     */
    public <T> CachedConfig<T> getMiddlewareConfigFromCache(String configKey) {

        LOG.debug("Getting config '{}' from table '{}'", configKey, DYNAMO_DB_TABLE_NAME);

        var response = this.getFromCache(configKey);

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
     * @param configKey         - key that we are placing the attribute value under.
     * @param config            - config we want to cache.
     * @param <T>               - config class.
     * @return - returns true if successfully cached the config.
     */
    public <T> boolean pushMiddlewareConfigToCache(String configKey, T config) {

        LOG.debug("Pushing config '{}' to table '{}' with the payload of '{}'", DYNAMO_DB_TABLE_NAME, configKey, config.toString());

        CachedConfig<T> cachedConfig;

        try {
            cachedConfig = new CachedConfig<>(configKey, config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this.pushToCache(cachedConfig.getDynamoDbPayload());
    }

    public Map<String, AttributeValue> getConfig() {
        final String key = "values";
        return this.getFromCache(key);
    }

    /**
     * Pushes a JwtClaim to dynamodb.
     *
     * @param jwtKey            - attribute key
     * @param claims            - jwt claims
     * @return - return true if successful.
     */
    public boolean pushJwtToCache(String jwtKey, JwtClaims claims) {
        CachedJwtClaims cachedJwt;
        try {
            cachedJwt = new CachedJwtClaims(jwtKey, claims);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return this.pushToCache(cachedJwt.getDynamoDbPayload());
    }

    /**
     * Grabs the JwtClaim from dynamo db.
     * We use the jwtKey as the projection expression.
     *
     * @param jwtKey            - attribute key used as a projection expression to get any existing keys found in the DB.
     * @return - returns JwtClaim and returns null if it does not exist.
     */
    public CachedJwtClaims getJwtFromCache(String jwtKey) {
        var response = this.getFromCache(jwtKey);

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
     * @param cachePayload      - hashmap of string attribute value.
     * @return - returns true if successful
     */
    private boolean pushToCache(Map<String, AttributeValue> cachePayload) {
        var res = this.dynamoClient.putItem(DYNAMO_DB_TABLE_NAME, cachePayload);
        return this.isSuccessResponse(res.getSdkHttpMetadata());
    }

    /**
     * Gets some attribute from a specified table based on a projection expression.
     *
     * @param projectionExpression - projection expression that specifies the attributes.
     * @return - returns attribute map if item is found and returns null if no data was found.
     */
    private Map<String, AttributeValue> getFromCache(String projectionExpression) {
        var table = this.dynamoDB.getTable(DYNAMO_DB_TABLE_NAME);
        LOG.debug("DynamoDB Table: {}", table.toString());


        var getItemReq = new GetItemRequest();
        getItemReq.setTableName(DYNAMO_DB_TABLE_NAME);
        getItemReq.setProjectionExpression(projectionExpression);

        var res = this.dynamoClient.getItem(getItemReq);

        if (this.isSuccessResponse(res.getSdkHttpMetadata()))
            return res.getItem();

        else return null;
    }

    private Map<String, AttributeValue> getBatchFromCache(List<String> attributeKeys) {
        var projectionExpression = new StringBuilder();

        for (int i = 0; i < attributeKeys.size(); i++) {
            var attributeKey = attributeKeys.get(i);
            projectionExpression.append(attributeKey);

            if (i != attributeKeys.size() - 1)
                projectionExpression.append(", ");
        }
        return this.getFromCache(projectionExpression.toString());
    }


    /**
     * Creates dynamo db table. We check if the table exists before creating one.
     *
     * @return - return true when a table was created. Returns false when there is a failure or the table already exists.
     */
    private boolean createCacheTable() throws JsonProcessingException {

        LOG.debug("Creating cache table '{}'", DYNAMO_DB_TABLE_NAME);

        if (this.doesTableExist(DYNAMO_DB_TABLE_NAME)) {
            LOG.debug("Table already exists... returning...");
            return false;
        }

        var attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("Id")
                .withAttributeType("N")
        );

        var keySchema = new ArrayList<KeySchemaElement>();
        keySchema.add(new KeySchemaElement()
                .withAttributeName("Id")
                .withKeyType(KeyType.HASH)
        );

        var createTableRequest = new CreateTableRequest()
                .withTableName(DYNAMO_DB_TABLE_NAME)
                .withKeySchema(keySchema)
                .withAttributeDefinitions(attributeDefinitions)
                .withProvisionedThroughput(new ProvisionedThroughput()
                        .withReadCapacityUnits(5L)
                        .withWriteCapacityUnits(6L));

        var res = this.dynamoClient.createTable(createTableRequest);
        return this.isSuccessResponse(res.getSdkHttpMetadata());
    }

    /**
     * DEBUG FUNCTION - will be changed or deprecated in the future.
     */
    public void deleteTable(String dynamoDbTableName) throws InterruptedException {

        if (!this.doesTableExist(dynamoDbTableName)) {
            LOG.debug("Table does not exist, no need to delete...");
            return;
        }

        var table = dynamoDB.getTable(dynamoDbTableName);
        table.delete();
        table.waitForDelete();
    }

    public boolean initCacheTable() throws JsonProcessingException {

        if (!this.tableInitiated) {
            this.tableInitiated = this.createCacheTable();
        }

        return this.tableInitiated;
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

    /**
     * Checks to see if the table exists.
     *
     * @param dynamoDbTableName - name of the table
     * @return - returns true if the table exists
     */
    public boolean doesTableExist(String dynamoDbTableName) {
        var tables = this.dynamoClient.listTables(TABLE_LIST_LIMIT);
        LOG.debug("Tables: {}", tables.getTableNames().toString());
        return tables.getTableNames().contains(dynamoDbTableName);
    }


}
