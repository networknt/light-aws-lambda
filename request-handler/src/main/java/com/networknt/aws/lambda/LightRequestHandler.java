package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class LightRequestHandler {
    public APIGatewayProxyResponseEvent interceptRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // JWT scope verification against the openapi specification.
        ScopeVerifier scopeVerifier = new ScopeVerifier();
        APIGatewayProxyResponseEvent responseEvent = scopeVerifier.verifyScope(input);
        if(responseEvent != null) return responseEvent;
        // OpenAPI schema validation
        LambdaSchemaValidator validator = new LambdaSchemaValidator();
        responseEvent = validator.validateRequest(input);
        if(responseEvent != null) return responseEvent;
        return null;
    }

    public APIGatewayProxyResponseEvent interceptResponse(APIGatewayProxyResponseEvent response) {
        return response;
    }
}
