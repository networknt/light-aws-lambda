package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.mservicetech.openapi.common.RequestEntity;
import com.mservicetech.openapi.common.Status;
;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * It is called in the Lambda framework to validate the request against the openapi.yaml specification.
 *
 * Each function will have the openapi.yaml packaged as configuration and this class will use it to
 * validate the request headers, query parameters, path parameters and body based on the json schema.
 *
 * The validateRequest is called by the request-handler that intercepts the request and response in the App.
 *
 * @author Steve Hu, Gavin Chen
 */
public class OpenApiValidator {
    static final Logger logger = LoggerFactory.getLogger(OpenApiValidator.class);
    static final String CONTENT_TYPE = "application/json";


    public OpenApiValidator() {
    }
    /**
     * Validate the request based on the openapi.yaml specification
     *
     * @param requestEvent request event
     * @return responseEvent if error and null if pass.
     */
    public APIGatewayProxyResponseEvent validateRequest(APIGatewayProxyRequestEvent requestEvent) {
        com.mservicetech.openapi.validation.OpenApiValidator openApiValidator = new com.mservicetech.openapi.validation.OpenApiValidator("openapi.yaml");
        RequestEntity requestEntity = new RequestEntity();
        requestEntity.setQueryParameters(requestEvent.getQueryStringParameters());
        requestEntity.setPathParameters(requestEvent.getPathParameters());
        requestEntity.setHeaderParameters(requestEvent.getHeaders());
        if (requestEvent.getBody()!=null) {
            requestEntity.setRequestBody(requestEvent.getBody());
            requestEntity.setContentType(CONTENT_TYPE);
        }
        Status status = openApiValidator.validateRequestPath(requestEvent.getPath(), requestEvent.getHttpMethod(), requestEntity);
        if (status !=null) {
            return createErrorResponse(status.getStatusCode(), status.getCode(), status.getDescription());
        }
        return null;
    }

    private APIGatewayProxyResponseEvent createErrorResponse(int statusCode, String errorCode, String errorMessage) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String body = "{\"statusCode\":" + statusCode + ",\"code\":\"" + errorCode + ",\"description\":\"" + errorMessage + "\"}";
        return new APIGatewayProxyResponseEvent()
                .withHeaders(headers)
                .withStatusCode(statusCode)
                .withBody(body);
    }


}
