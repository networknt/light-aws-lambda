package com.networknt.aws.lambda;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    private HttpUtils() {
        throw new IllegalStateException("HttpUtils is a utility class");
    }

    public static HttpURLConnection get(String endpoint) throws IOException {
        URL url = new URL(endpoint);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        // Execute HTTP Call
        connection.getResponseCode();

        return connection;
    }

    public static HttpURLConnection post(String endpoint, String message) throws IOException {
        URL url = new URL(endpoint);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");

        connection.setDoOutput(true);

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.write(message.getBytes(StandardCharsets.UTF_8));

        out.flush();
        out.close();

        // Execute HTTP Call
        connection.getResponseCode();

        return connection;
    }
}
