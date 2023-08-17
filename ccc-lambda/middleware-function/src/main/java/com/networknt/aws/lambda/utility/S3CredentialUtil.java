package com.networknt.aws.lambda.utility;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

public class S3CredentialUtil {

    private static SecretsManagerClient SECRET_MANAGER_CLIENT;

    private static SecretsManagerClient getInstance() {
        if (SECRET_MANAGER_CLIENT == null) {
            SECRET_MANAGER_CLIENT = SecretsManagerClient.builder()
                    .region(Region.of(System.getProperty("AWS_REGION")))
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


}
