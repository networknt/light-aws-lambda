package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import com.networknt.utility.NioUtils;

public class Main {

    private static final String REQUEST_ID_HEADER = "lambda-runtime-aws-request-id";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static void main(String[] args) throws IOException {
        while(true) {
            String endpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");

            InvocationResponse invocation = getInvocation(endpoint);

            try {
                Authorizer authorizer = new Authorizer();

                // Actual invoke of the Authorizer
                AuthPolicy response = authorizer.handleRequest(invocation.getEvent(), new LambdaContext(invocation.getRequestId()));

                // Post to Lambda success endpoint
                HttpUtils.post(
                        String.format("http://%s/2018-06-01/runtime/invocation/%s/response", endpoint, invocation.getRequestId()),
                        OBJECT_MAPPER.writeValueAsString(response)
                );
            } catch (Exception t) {
                String response = OBJECT_MAPPER.writeValueAsString(
                        DefaultResponse.builder()
                                .message(t.getMessage())
                                .build()
                );

                t.printStackTrace();

                // Post to Lambda error endpoint
                HttpUtils.post(
                        String.format("http://%s/2018-06-01/runtime/invocation/%s/error", endpoint, invocation.getRequestId()),
                        response
                );
            }
        }
    }

    private static InvocationResponse getInvocation(String endpoint) throws IOException {
        HttpURLConnection connection = HttpUtils.get(
                String.format("http://%s/2018-06-01/runtime/invocation/next", endpoint)
        );

        String response = NioUtils.toString(connection.getInputStream());

        String requestId = connection.getHeaderField(REQUEST_ID_HEADER);

        APIGatewayProxyRequestEvent event = OBJECT_MAPPER.readValue(response, APIGatewayProxyRequestEvent.class);

        return InvocationResponse.builder()
                .requestId(requestId)
                .event(event)
                .build();
    }
}
