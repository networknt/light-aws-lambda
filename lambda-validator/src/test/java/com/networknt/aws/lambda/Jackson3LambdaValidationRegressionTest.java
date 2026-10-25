package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.networknt.config.Config;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Jackson3LambdaValidationRegressionTest {
    @Test
    void validatesAnEventBoundByTheFrameworkMapper() {
        var request = Config.getInstance().getMapper().readValue(
                "{\"path\":\"/pets\",\"httpMethod\":\"GET\",\"queryStringParameters\":{\"includeCode\":\"true\"}}",
                APIGatewayProxyRequestEvent.class);
        assertNull(new LambdaSchemaValidator().validateRequest(request));
    }

    @Test
    void keepsMalformedBodyErrorsHandled() {
        var request = new APIGatewayProxyRequestEvent().withPath("/pets").withHttpMethod("POST")
                .withHeaders(Map.of("Content-Type", "application/json")).withBody("{");
        var response = new LambdaSchemaValidator().validateRequest(request);
        assertNotNull(response);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("ERR10015"), response.getBody());
    }

    @Test
    void keepsValidationMessagesAndMethodErrorsStable() {
        var validator = new LambdaSchemaValidator();
        var request = new APIGatewayProxyRequestEvent().withPath("/pets").withHttpMethod("GET")
                .withQueryStringParameters(Map.of("includeCode", "wrong"));
        var response = validator.validateRequest(request);
        assertEquals(400, response.getStatusCode());
        assertTrue(response.getBody().contains("/includeCode: string found, boolean expected"), response.getBody());
        request.setHttpMethod("PATCH");
        assertEquals(405, validator.validateRequest(request).getStatusCode());
    }
}
