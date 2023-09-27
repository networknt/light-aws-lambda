package com.networknt.aws.lambda.cache;

import com.amazonaws.ResponseMetadata;
import com.amazonaws.http.SdkHttpMetadata;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.networknt.aws.lambda.utility.LambdaEnvVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class LambdaCache {
    private static final Logger LOG = LoggerFactory.getLogger(LambdaCache.class);
    private static final String DYNAMO_DB_JWT_ATTR_KEY = "jwt";
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

    public static String getDynamoDbTableName(String applicationId) {
        String dynamoDbTableName = applicationId;
        dynamoDbTableName = dynamoDbTableName.replace("-", "_");
        dynamoDbTableName = dynamoDbTableName.toUpperCase();
        return dynamoDbTableName;
    }

    public <T> Map<String, AttributeValue> getMiddlewareConfig(String dynamoDbTableName, T config) {
        var projection = config.getClass().getSimpleName();
        return getFromCache(dynamoDbTableName, projection);
    }

    public boolean pushJwtToCache(String dynamoDbTableName, String jwt) {
        var cachePayload = new HashMap<String, AttributeValue>();
        var attributeValue = new AttributeValue(jwt);
        cachePayload.put(DYNAMO_DB_JWT_ATTR_KEY, attributeValue);
        return this.pushToCache(dynamoDbTableName, cachePayload);
    }

    public Map<String, AttributeValue> getJwtFromCache(String dynamoDbTableName, String jwt) {
        var cachePayload = new HashMap<String, AttributeValue>();
        var attributeValue = new AttributeValue(jwt);
        cachePayload.put(DYNAMO_DB_JWT_ATTR_KEY, attributeValue);
        return this.getFromCache(dynamoDbTableName, cachePayload);
    }

    private boolean pushToCache(String dynamoDbTableName, Map<String, AttributeValue> cachePayload) {
        var res = this.amazonDynamoDB.putItem(dynamoDbTableName, cachePayload);
        return this.isSuccessResponse(res.getSdkHttpMetadata());
    }

    private Map<String, AttributeValue> getFromCache(String dynamoDbTableName, Map<String, AttributeValue> cachePayload) {
        var res = this.amazonDynamoDB.getItem(dynamoDbTableName, cachePayload);

        if (this.isSuccessResponse(res.getSdkHttpMetadata())) {
               return res.getItem();

        } else return null;
    }

    private Map<String, AttributeValue> getFromCache(String dynamoDbTableName, String projectionExpression) {
        var getItemReq = new GetItemRequest();
        getItemReq.setTableName(dynamoDbTableName);
        getItemReq.setProjectionExpression(projectionExpression);
        var res = this.amazonDynamoDB.getItem(getItemReq);

        if (this.isSuccessResponse(res.getSdkHttpMetadata())) {
            return res.getItem();

        } else return null;
    }

    private boolean isSuccessResponse(SdkHttpMetadata res) {
        return res.getHttpStatusCode() >= 200 && res.getHttpStatusCode() < 300;
    }


}
