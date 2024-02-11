package com.networknt.aws.lambda.admin;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

public class HealthCheckHandlerTest {
    @Test
    public void testHealthCheck() {
        HealthCheckHandler handler = new HealthCheckHandler();
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(null, null);
        assert(responseEvent != null);
        assert(responseEvent.getStatusCode() == 200);
        assert(responseEvent.getBody().equals(HealthCheckHandler.HEALTH_RESULT_OK));
    }
}
