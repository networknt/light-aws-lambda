package com.networknt.aws.lambda.cache;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

public abstract class CachedPayload {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    protected final String attributeKey;
    protected final String attributeValue;

    protected CachedPayload(String attributeKey, String attributeValue) {
        this.attributeValue = attributeValue;
        this.attributeKey = attributeKey;
    }

    public String getAttributeKey() {
        return attributeKey;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public Map<String, AttributeValue> getDynamoDbPayload() {
        var dynamoDbPayload = new HashMap<String, AttributeValue>();
        dynamoDbPayload.put(attributeKey, new AttributeValue(attributeValue));
        return dynamoDbPayload;
    }
}
