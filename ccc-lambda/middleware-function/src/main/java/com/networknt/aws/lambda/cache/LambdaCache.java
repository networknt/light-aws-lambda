package com.networknt.aws.lambda.cache;

import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.utility.LambdaEnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LambdaCache {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaCache.class);
    private static final String DYNAMO_DB_TABLE_NAME = "LAMBDA_NATIVE_PROXY";
    private static final String JWK_KEY = "JWK";
    private static final String JWT_KEY = "JWT";
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

    private void updateStringItem(String applicationId, String updateAttrKey, String updateAttrValue) {
        LOG.debug("Updating table entry of id: {}, with attribute key {} and value {}", applicationId, updateAttrKey, updateAttrValue);

        Table table = dynamoDB.getTable(DYNAMO_DB_TABLE_NAME);
        var item = table.getItem("Id", applicationId);

        if (item != null && item.getString(updateAttrKey) != null) {
            LOG.debug("Update spec....");

            // TODO - Update string value

        } else {
            /* primary key for item */
            var itemKey = new HashMap<String, AttributeValue>();
            itemKey.put("Id", new AttributeValue().withS(applicationId));

            /* attribute we are adding to item */
            var attributeUpdates = new HashMap<String, AttributeValueUpdate>();
            var update = new AttributeValueUpdate().withAction(AttributeAction.PUT).withValue(new AttributeValue().withS(updateAttrValue));
            attributeUpdates.put(updateAttrKey, update);

            /* send update request */
            var updateItemRequest = new UpdateItemRequest().withTableName(DYNAMO_DB_TABLE_NAME).withKey(itemKey).withAttributeUpdates(attributeUpdates);
            var res = this.dynamoClient.updateItem(updateItemRequest);
            LOG.debug("RESULT: {}", res.toString());
        }
    }

    public String getJwk(String applicationId) {
        final Table table = dynamoDB.getTable(DYNAMO_DB_TABLE_NAME);

        /* see if the table contains our application id. */
        /* If not found, return null because we don't have a cache yet! */
        Item entry;
        try {
            entry = table.getItem("Id", applicationId);
            if (entry == null)
                return null;
        } catch (NullPointerException e) {
            return null;
        }

        return entry.getString(JWK_KEY);
    }

    public void setJwk(String applicationId, String key) {
        this.updateStringItem(applicationId, JWK_KEY, key);
    }


    /**
     * Creates dynamo db table. We check if the table exists before creating one.
     *
     * @return - return true when a table was created. Returns false when there is a failure or the table already exists.
     */
    private boolean createCacheTable() {

        LOG.debug("Attempting to create new cache table '{}'", DYNAMO_DB_TABLE_NAME);

        if (this.doesTableExist()) {
            LOG.debug("Table already exists... returning...");
            return false;
        }

        var attributeDefinitions = new ArrayList<AttributeDefinition>();
        attributeDefinitions.add(new AttributeDefinition()
                .withAttributeName("Id")
                .withAttributeType("S")
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
    public void deleteTable() throws InterruptedException {

        if (!this.doesTableExist()) {
            LOG.debug("Table does not exist so we do not need to delete it....");
            return;
        }

        var table = dynamoDB.getTable(DYNAMO_DB_TABLE_NAME);
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
     * @return - returns true if the table exists
     */
    public boolean doesTableExist() {
        var tables = this.dynamoClient.listTables(TABLE_LIST_LIMIT);
        return tables.getTableNames().contains(DYNAMO_DB_TABLE_NAME);
    }


}
