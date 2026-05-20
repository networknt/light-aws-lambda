package com.networknt.aws.lambda;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    private HttpUtils() {
        throw new IllegalStateException("HttpUtils is a utility class");
    }

    public static HttpURLConnection get(URI endpoint) throws IOException {
        HttpURLConnection connection = openConnection(endpoint);
        connection.setRequestMethod("GET");

        // Execute HTTP Call
        connection.getResponseCode();

        return connection;
    }

    @Deprecated(since = "2.3.5", forRemoval = false)
    public static HttpURLConnection get(String endpoint) throws IOException {
        return get(toUri(endpoint));
    }

    public static HttpURLConnection post(URI endpoint, String message) throws IOException {
        HttpURLConnection connection = openConnection(endpoint);
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

    @Deprecated(since = "2.3.5", forRemoval = false)
    public static HttpURLConnection post(String endpoint, String message) throws IOException {
        return post(toUri(endpoint), message);
    }

    private static HttpURLConnection openConnection(URI endpoint) throws IOException {
        if (endpoint == null || !"http".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null) {
            throw new IllegalArgumentException("Endpoint must be an absolute HTTP URI.");
        }

        return (HttpURLConnection) endpoint.toURL().openConnection();
    }

    private static URI toUri(String endpoint) throws IOException {
        try {
            return new URI(endpoint);
        } catch (URISyntaxException e) {
            MalformedURLException malformedException = new MalformedURLException("Invalid endpoint URI.");
            malformedException.initCause(e);
            throw malformedException;
        }
    }
}
