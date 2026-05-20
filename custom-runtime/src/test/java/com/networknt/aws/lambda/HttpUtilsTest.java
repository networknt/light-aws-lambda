package com.networknt.aws.lambda;

import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpUtilsTest {

    @Test
    void keepsStringOverloadsForBackwardCompatibility() throws NoSuchMethodException {
        assertEquals(HttpURLConnection.class, HttpUtils.class.getMethod("get", String.class).getReturnType());
        assertEquals(HttpURLConnection.class, HttpUtils.class.getMethod("post", String.class, String.class).getReturnType());
    }
}
