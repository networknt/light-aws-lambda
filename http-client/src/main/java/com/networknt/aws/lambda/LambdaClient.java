package com.networknt.aws.lambda;

import com.networknt.config.Config;
import com.networknt.exception.ClientException;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.status.Status;
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

public class LambdaClient {
    private static final Logger logger = LoggerFactory.getLogger(LambdaClient.class);
    private static final String TRUST_STORE_PROPERTY = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PASSWORD_PROPERTY = "javax.net.ssl.trustStorePassword";
    private static final String TRUST_STORE_PASS = "truststorePass";
    private static final String TRUST_STORE_NAME = "truststoreName";
    private static final String JWK_URL = "jwkUrl";
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";
    private static final String OAUTH_SERVER_URL_ERROR = "ERR10056";

    private static Map<String, Object> configMap = null;
    private static final LambdaClient INSTANCE = new LambdaClient();

    private LambdaClient() {
    }
    public static LambdaClient getInstance(String stage) {
        Configuration configuration = new Configuration();
        configMap  = configuration.getConfigMap(stage);
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
                trustStorePass = (String) configMap.get(TRUST_STORE_PASS);
                if(trustStorePass == null) {
                    logger.error(new Status(CONFIG_PROPERTY_MISSING, TRUST_STORE_PASS, "app.yml").toString());
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
        try (InputStream stream = Configuration.class.getResourceAsStream(name)) {
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
     * @throws ClientException throw exception if communication with the service fails.
     */
    public static String getKey() throws ClientException {
        String jwkUrl = (String)configMap.get(JWK_URL);
        if(jwkUrl == null) {
            throw new ClientException(new Status(OAUTH_SERVER_URL_ERROR, "key"));
        }
        try {
            // The key client is used only during the server startup or jwt key is rotated. Don't cache the keyClient.
            HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(2000))
                    .sslContext(createSSLContext());
            clientBuilder.version(HttpClient.Version.HTTP_2);
            HttpClient keyClient = clientBuilder.build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(jwkUrl));
            HttpRequest request = requestBuilder.build();

            CompletableFuture<HttpResponse<String>> response =
                    keyClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            return response.thenApply(HttpResponse::body).get(2000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

}
