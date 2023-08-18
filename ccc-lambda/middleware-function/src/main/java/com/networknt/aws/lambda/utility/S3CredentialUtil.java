package com.networknt.aws.lambda.utility;

import com.amazonaws.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class S3CredentialUtil {

    private static final Logger LOG = LoggerFactory.getLogger(S3CredentialUtil.class);

    private static SecretsManagerClient SECRET_MANAGER_CLIENT;

    private static SecretsManagerClient getInstance() {
        if (SECRET_MANAGER_CLIENT == null) {
            SECRET_MANAGER_CLIENT = SecretsManagerClient.builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .build();
        }

        return SECRET_MANAGER_CLIENT;
    }


    public static String getCachedJWK(String secretName) {
        var client = getInstance();
        var jwkValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse res;
        try {
            res = client.getSecretValue(jwkValueRequest);
        } catch (Exception e) {
            throw e;
        }

        if (res != null)
            return res.secretString();
        else return null;
    }

    public static String getCachedJWT(String applicationId) {
        final var secretName = applicationId + "-jwt";
        final var client = getInstance();
        final var tokenValueRequest = GetSecretValueRequest.builder()
                .secretId(secretName)
                .build();

        GetSecretValueResponse res;
        try {
            res = client.getSecretValue(tokenValueRequest);
        } catch (Exception e) {
            throw e;
        }

        if (res != null)
            return res.secretString();

        else return null;


    }

    // TODO - remove the logging statements once testing is done.
    public static String getLambdaCachedJWK(String secretName) {

        LOG.debug("Getting secretName: {}", secretName);

        URL url = null;
        try {
            url = new URL("http://localhost:2773/secretsmanager/get?secretId=" + secretName);
        } catch (MalformedURLException e) {
            LOG.error("Error creating new URL: {}", e.getMessage(), e);
            return null;
        }

        LOG.debug("Using URL: {}", url);

        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            LOG.error("Error trying to open connection: {}", e.getMessage(), e);
            return null;
        }

        LOG.debug("AWS_SESSION_TOKEN: {}", System.getenv("AWS_SESSION_TOKEN"));
        con.setRequestProperty("X-Aws-Parameters-Secrets-Token", System.getenv("AWS_SESSION_TOKEN"));
        try {
            con.setRequestMethod("GET");
        } catch (ProtocolException e) {
            LOG.error("Error settings request method: {}", e.getMessage(), e);
            return null;
        }

        int statusCode = 0;
        try {
            statusCode = con.getResponseCode();
        } catch (IOException e) {
            LOG.error("Error getting response code: {}", e.getMessage(), e);
            return null;
        }

        LOG.debug("StatusCode: {}", statusCode);

        InputStream content = null;
        try {
            content = (InputStream) con.getContent();
        } catch (IOException e) {
            LOG.error("Error getting response content: {}", e.getMessage(), e);
            return null;
        }

        String fullContent = null;
        try {
            fullContent = IOUtils.toString(content);
        } catch (IOException e) {
            LOG.error("Failed to parse content stream: {}", e.getMessage(), e);
            return null;
        }

        LOG.debug("Content: {}", fullContent);
        return fullContent;
    }


}
