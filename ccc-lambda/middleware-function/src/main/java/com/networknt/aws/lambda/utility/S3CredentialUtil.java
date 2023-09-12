package com.networknt.aws.lambda.utility;

import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
//import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
//import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class S3CredentialUtil {

    public enum SecretType {
        JWK, JWT, OTHER
    }

    private static final Logger LOG = LoggerFactory.getLogger(S3CredentialUtil.class);

    //private static SecretsManagerClient SECRET_MANAGER_CLIENT;

//    private static SecretsManagerClient getInstance() {
//
//        if (SECRET_MANAGER_CLIENT == null) {
//            SECRET_MANAGER_CLIENT = SecretsManagerClient.builder().region(Region.of(System.getenv("AWS_REGION"))).build();
//        }
//
//        return SECRET_MANAGER_CLIENT;
//    }
//
//    public static boolean postCachedSecret(String secretName, String secret) {
//        final var request = CreateSecretRequest.builder()
//                .name(secretName)
//                .secretString(secret)
//                .build();
//        final var client = getInstance();
//        final var response = client.createSecret(request);
//        return response.sdkHttpResponse().isSuccessful();
//    }
//
//    public static String getCachedSecret(String secretName) {
//        final var client = getInstance();
//        final var tokenValueRequest = GetSecretValueRequest.builder().secretId(secretName).build();
//
//        GetSecretValueResponse res;
//        try {
//            res = client.getSecretValue(tokenValueRequest);
//        } catch (Exception e) {
//            LOG.error("Error getting secret: {}", e.getMessage(), e);
//            return null;
//        }
//
//        if (res != null)
//            return res.secretString();
//
//        else return null;
//
//
//    }

//    public static String getLambdaCachedSecret(String applicationId, SecretType type) {
//        switch (type) {
//            case JWK:
//                return getCachedSecret(applicationId + "-jwk");
//            case JWT:
//                return getCachedSecret(applicationId + "-jwt");
//            case OTHER:
//            default:
//                return getCachedSecret(applicationId);
//        }
//    }
//
//    public static boolean postLambdaCachedSecret(String applicationId, String secret, SecretType type) {
//        switch (type) {
//            case JWK:
//                return postCachedSecret(applicationId + "-jwk", secret);
//            case JWT:
//                return postCachedSecret(applicationId + "-jwt", secret);
//            case OTHER:
//            default:
//                return postCachedSecret(applicationId, secret);
//        }
//    }

    // TODO - test speed using ARN layer vs SDK
    public static boolean postLambdaS3Secret(String secretName, String secret) {

        URL url;
        try {
            url = new URL("http://localhost:2773/secretsmanager/post");
        } catch (MalformedURLException e) {
            LOG.error("Error creating new URL: {}", e.getMessage(), e);
            return false;
        }

        HttpURLConnection con;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            LOG.error("Error trying to open connection: {}", e.getMessage(), e);
            return false;
        }

        con.setRequestProperty(HeaderKey.PARAMETER_SECRET_TOKEN, System.getenv(LambdaEnvVariables.LAMBDA_SESSION_TOKEN));
        con.setRequestProperty(HeaderKey.CONTENT_TYPE, HeaderValue.APPLICATION_AMZ);
        con.setRequestProperty(HeaderKey.AMZ_TARGET, "secretsmanager.CreateSecret");
        con.setDoInput(true);

        try {
            con.setRequestMethod("POST");
        } catch (ProtocolException e) {
            LOG.error("Error settings request method: {}", e.getMessage(), e);
            return false;
        }

        final String jsonInputString = "{" + "\"Name\": \"" + secretName + "\"" + "\"Description\": \"JWT Secret Okta\"" + "\"SecretString\": \"" + secret + "\"" + "}";

        try (OutputStream os = con.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);

        } catch (IOException e) {
            LOG.error("Error adding request body: {}", e.getMessage(), e);
            return false;
        }

        int statusCode;
        try {
            statusCode = con.getResponseCode();
        } catch (IOException e) {
            LOG.error("Error getting response code: {}", e.getMessage(), e);
            return false;
        }

        return statusCode >= 200 && statusCode < 300;


//        POST / HTTP/1.1
//        Host: secretsmanager.region.domain
//        Accept-Encoding: identity
//        X-Amz-Target: secretsmanager.CreateSecret
//        Content-Type: application/x-amz-json-1.1
//        User-Agent: <user-agent-string>
//        X-Amz-Date: <date>
//        Authorization: AWS4-HMAC-SHA256 Credential=<credentials>,SignedHeaders=<headers>, Signature=<signature>
//        Content-Length: <payload-size-bytes>
//
//        {
//        "Name": "MyTestDatabaseSecret",
//        "Description": "My test database secret created with the CLI",
//        "SecretString": "{\"username\":\"david\",\"password\":\"EXAMPLE-PASSWORD\"}", "ClientRequestToken": "EXAMPLE1-90ab-cdef-fedc-ba987SECRET1"
//        }

    }

    // TODO - test speed using ARN layer vs sdk.
    public static String getLambdaS3Secret(String secretName) {
        URL url;
        try {
            url = new URL("http://localhost:2773/secretsmanager/get?secretId=" + secretName);
        } catch (MalformedURLException e) {
            LOG.error("Error creating new URL: {}", e.getMessage(), e);
            return null;
        }

        HttpURLConnection con;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            LOG.error("Error trying to open connection: {}", e.getMessage(), e);
            return null;
        }

        con.setRequestProperty("X-Aws-Parameters-Secrets-Token", System.getenv(LambdaEnvVariables.LAMBDA_SESSION_TOKEN));
        try {
            con.setRequestMethod("GET");
        } catch (ProtocolException e) {
            LOG.error("Error settings request method: {}", e.getMessage(), e);
            return null;
        }

        int statusCode;
        try {
            statusCode = con.getResponseCode();
        } catch (IOException e) {
            LOG.error("Error getting response code: {}", e.getMessage(), e);
            return null;
        }

        if (statusCode >= 200 && statusCode < 300) {
            InputStream content;

            try {
                content = (InputStream) con.getContent();
            } catch (IOException e) {
                LOG.error("Error getting response content: {}", e.getMessage(), e);
                return null;
            }

            String fullContent;
            try {
                fullContent = IOUtils.toString(content);
            } catch (IOException e) {
                LOG.error("Failed to parse content stream: {}", e.getMessage(), e);
                return null;
            }

            return fullContent;

        } else return null;
    }


}
