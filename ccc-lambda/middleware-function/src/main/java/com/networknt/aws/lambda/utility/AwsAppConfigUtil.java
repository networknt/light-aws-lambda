package com.networknt.aws.lambda.utility;

import software.amazon.awssdk.services.appconfigdata.AppConfigDataClient;
import software.amazon.awssdk.services.appconfigdata.model.GetLatestConfigurationRequest;
import software.amazon.awssdk.services.appconfigdata.model.StartConfigurationSessionRequest;

import java.net.HttpURLConnection;
import java.net.URL;

public class AwsAppConfigUtil {

    private static AppConfigDataClient APP_CONFIG_DATA_CLIENT;
    private static String SESSION_TOKEN;

    private static AppConfigDataClient getInstance() {

//        if (APP_CONFIG_DATA_CLIENT == null) {
//            APP_CONFIG_DATA_CLIENT = AppConfigDataClient.create();
//        }

        return APP_CONFIG_DATA_CLIENT;
    }

    public static String getConfiguration(final String applicationId, final String env, final String profile) {
        try {
//            if (SESSION_TOKEN == null) {
//                SESSION_TOKEN = getInstance().startConfigurationSession(
//                        StartConfigurationSessionRequest.builder()
//                                .applicationIdentifier(applicationId)
//                                .environmentIdentifier(env)
//                                .configurationProfileIdentifier(profile)
//                                .build()
//                ).initialConfigurationToken();
//            }
//
//            System.out.println(SESSION_TOKEN);
//
//            var configResponse = getInstance().getLatestConfiguration(
//                    GetLatestConfigurationRequest.builder()
//                            .configurationToken(SESSION_TOKEN)
//                            .build()
//            );
//
//            if (configResponse.sdkHttpResponse().isSuccessful()) {
//                SESSION_TOKEN = configResponse.nextPollConfigurationToken();
//                return configResponse.configuration().asUtf8String();
//
//            }
            URL url = new URL("http://localhost:2772");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            System.out.println("status code: " + status);
        } catch (Throwable e) {
            // Do nothing
        }

        return null;
    }

}
