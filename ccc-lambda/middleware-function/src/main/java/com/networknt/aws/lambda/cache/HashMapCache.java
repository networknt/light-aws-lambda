package com.networknt.aws.lambda.cache;

import java.util.HashMap;
import java.util.Map;

// TODO - combine Lambda cache and existing concurrent hashmap cache
public class HashMapCache<T> {

    Map<String, T> cache;

    public HashMapCache() {
        this.cache = new HashMap<>();
    }
}
