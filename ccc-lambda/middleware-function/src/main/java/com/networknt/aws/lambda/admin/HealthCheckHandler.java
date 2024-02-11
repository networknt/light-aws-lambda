package com.networknt.aws.lambda.admin;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.config.Config;
import com.networknt.health.HealthConfig;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HealthCheckHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    public static final String HEALTH_RESULT_OK = "OK";
    public static final String HEALTH_RESULT_ERROR = "ERROR";

    static final Logger logger = LoggerFactory.getLogger(HealthCheckHandler.class);
    static HealthConfig config = HealthConfig.load();

    public HealthCheckHandler() {
        logger.info("HealthCheckHandler is constructed");
        ModuleRegistry.registerModule(HealthConfig.CONFIG_NAME, HealthCheckHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(HealthConfig.CONFIG_NAME), null);
    }

    /**
     * Constructor with configuration for testing purpose only
     * @param cfg HealthConfig
     */
    public HealthCheckHandler(HealthConfig cfg) {
        config = cfg;
        logger.info("HealthCheckHandler is constructed");
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, Context context) {
        if(logger.isTraceEnabled()) logger.trace("HealthCheckHandler.handleRequest starts.");
        String result = HEALTH_RESULT_OK;
        if(config.isDownstreamEnabled()) {
            result = backendHealth();
        }
        // for security reason, we don't output the details about the error. Users can check the log for the failure.
        var responseEvent = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain");
        responseEvent.setHeaders(headers);
        if(HEALTH_RESULT_ERROR.equals(result)) {
            responseEvent.setStatusCode(400);
            responseEvent.setBody(HEALTH_RESULT_ERROR);
            if(logger.isTraceEnabled()) logger.trace("HealthCheckHandler.handleRequest ends with an error.");
        } else {
            responseEvent.setStatusCode(200);
            responseEvent.setBody(HEALTH_RESULT_OK);
            if(logger.isTraceEnabled()) logger.trace("HealthCheckHandler.handleRequest ends.");
        }
        return responseEvent;
    }

    /**
     * Try to access the configurable /health endpoint on the backend Lambda. return OK if a success response is returned.
     * Otherwise, ERROR is returned.
     *
     * @return result String of OK or ERROR.
     */
    private String backendHealth() {
        String result = HEALTH_RESULT_OK;
        long start = System.currentTimeMillis();
        // TODO call the backend health check endpoint

        long responseTime = System.currentTimeMillis() - start;
        if(logger.isDebugEnabled()) logger.debug("Downstream health check response time = " + responseTime);
        return result;
    }

}
