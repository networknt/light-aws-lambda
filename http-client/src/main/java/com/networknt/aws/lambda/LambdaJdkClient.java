package com.networknt.aws.lambda;

import org.owasp.encoder.Encode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.*;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LambdaJdkClient {
    private static final Logger logger = LoggerFactory.getLogger(LambdaJdkClient.class);
    private static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_PASS = "truststorePass";
    private static final String TRUST_STORE_NAME = "truststoreName";
    private static final String JWK_URL = "jwkUrl";

    private static Map<String, Object> configMap = null;
    private static final LambdaJdkClient INSTANCE = new LambdaJdkClient();

    private LambdaJdkClient() {
    }
    public static LambdaJdkClient getInstance(String stage) {
        Configuration configuration = Configuration.getInstance();
        configMap  = configuration.getStageConfig(stage);
        return INSTANCE;
    }

    /**
     * create ssl context using specified truststore
     *
     * @return SSLContext
     * @throws IOException IOException
     */
    @SuppressWarnings("unchecked")
    public static SSLContext createSSLContext() throws IOException {
        SSLContext sslContext = null;
        KeyManager[] keyManagers = null;
        TrustManager[] trustManagers = null;
        try {
            // load trust store, this is the server public key certificate first check if
            // javax.net.ssl.trustStore system properties is set. It is only necessary if
            // the server certificate doesn't have the entire chain.
            String trustStoreName = System.getProperty(TRUST_STORE_PROPERTY);
            String trustStorePass = System.getProperty(TRUST_STORE_PASSWORD_PROPERTY);
            if (trustStoreName != null && trustStorePass != null) {
                if(logger.isInfoEnabled()) logger.info("Loading trust store from system property at " + Encode.forJava(trustStoreName));
            } else {
                trustStoreName = (String) configMap.get(TRUST_STORE_NAME);
                if(trustStoreName == null) {
                    logger.error("ERR10057 Config property truststoreName is missing in app.xml");
                }
                trustStorePass = (String) configMap.get(TRUST_STORE_PASS);
                if(trustStorePass == null) {
                    logger.error("ERR10057 Config property truststorePass is missing in app.xml");
                }
                if(logger.isInfoEnabled()) logger.info("Loading trust store from config at " + Encode.forJava(trustStoreName));
            }
            if (trustStoreName != null && trustStorePass != null) {
                KeyStore trustStore = loadTrustStore(trustStoreName, trustStorePass.toCharArray());
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
                trustManagers = trustManagerFactory.getTrustManagers();
            }
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            throw new IOException("Unable to initialise TrustManager[]", e);
        }
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IOException("Unable to create and initialise the SSLContext", e);
        }

        return sslContext;
    }

    public static KeyStore loadTrustStore(final String name, final char[] password) {
        try (InputStream stream = Configuration.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) {
                String message = "Unable to load truststore '" + name + "', please provide the truststore matching the configuration in app.yml to enable TLS connection.";
                if (logger.isErrorEnabled()) {
                    logger.error(message);
                }
                throw new RuntimeException(message);
            }
            KeyStore loadedKeystore = KeyStore.getInstance("JKS");
            loadedKeystore.load(stream, password);
            return loadedKeystore;
        } catch (Exception e) {
            logger.error("Unable to load truststore " + name, e);
            throw new RuntimeException("Unable to load truststore " + name, e);
        }
    }

    /**
     * Get the certificate from key distribution service of OAuth 2.0 provider with the kid.
     *
     * @return String of the certificate
     */
    public static String getKey() {
        String jwkUrl = (String)configMap.get(JWK_URL);
        if(jwkUrl == null) {
            logger.error("ERR10057 Config property jwkUrl is missing in app.xml");
        }
        try {
            // The key client is used only during the server startup or jwt key is rotated. Don't cache the keyClient.
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(3000))
                    .sslContext(createSSLContext());
            clientBuilder.version(HttpClient.Version.HTTP_2);
            HttpClient keyClient = clientBuilder.build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(jwkUrl));
            HttpRequest request = requestBuilder.build();

            CompletableFuture<HttpResponse<String>> response =
                    keyClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            return response.thenApply(HttpResponse::body).get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
    }

}
