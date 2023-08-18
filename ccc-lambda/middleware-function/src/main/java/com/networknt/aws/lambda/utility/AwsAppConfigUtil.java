package com.networknt.aws.lambda.utility;

import java.net.HttpURLConnection;
import java.net.URL;

public class AwsAppConfigUtil {

    public static String getConfiguration(final String applicationId, final String env, final String profile) {
        try {
            // localhost:2772 is the url of the AppConfig extension layer when used in lambda
            URL url = new URL("http://localhost:2772/applications/" + applicationId + "/environments/" + env + "/configurations/" + profile);
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
