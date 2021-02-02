package com.networknt.aws.lambda;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;

import com.networknt.utility.NioUtils;

public class Runtime {
    private static final String REQUEST_ID_HEADER = "lambda-runtime-aws-request-id";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    public static void main(String[] args) throws IOException {
        while(true) {
            String endpoint = System.getenv("AWS_LAMBDA_RUNTIME_API");
            InvocationResponse invocation = getInvocation(endpoint);
            String handlerName = System.getenv("_HANDLER");
            int pos = handlerName.indexOf("::");
            if (pos > 0) {
                handlerName = handlerName.substring(0, pos);
            }
            Class<?> clazz = null;
            try {
                clazz = Class.forName(handlerName);
                Object handler = clazz.getConstructor().newInstance();
                APIGatewayProxyResponseEvent response = null;
                if(handler instanceof RequestHandler) {
                    response = (APIGatewayProxyResponseEvent)((RequestHandler)handler).handleRequest(invocation.getEvent(), new LambdaContext(invocation.getRequestId()));
                }
                String result = OBJECT_MAPPER.writeValueAsString(response);
                // Post to Lambda success endpoint
                HttpUtils.post(String.format("http://%s/2018-06-01/runtime/invocation/%s/response", endpoint, invocation.getRequestId()), result);
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

    /**
     * Find {@link RequestHandler} "handleRequest".
     *
     * @param clazz {@link Class}
     * @param methodName {@link String}
     * @return {@link Method}
     */
    private static Method findRequestHandlerMethod(final Class<?> clazz, final String methodName) {
        Method method = null;
        for (Method m : clazz.getMethods()) {
            if (m.getName().equalsIgnoreCase(methodName)) {
                method = m;
                break;
            }
        }
        return method;
    }

    /**
     * Get the Parameter Type of the Object.
     *
     * @param object {@link Object}
     * @param method {@link Method}
     * @return {@link Class}
     * @throws ClassNotFoundException ClassNotFoundException
     */
    private static Class<?> getParameterType(final Object object, final Method method)
            throws ClassNotFoundException {
        Parameter parameter = method.getParameters()[0];
        Class<?> parameterType = parameter.getType();

        if (Object.class.equals(parameterType)) {

            Type[] types = object.getClass().getGenericInterfaces();
            if (types.length > 0 && types[0] instanceof ParameterizedType) {
                ParameterizedType p = (ParameterizedType) types[0];
                if (p.getActualTypeArguments().length > 0) {
                    parameterType = Class.forName(p.getActualTypeArguments()[0].getTypeName());
                }
            }
        }

        return parameterType;
    }

}
