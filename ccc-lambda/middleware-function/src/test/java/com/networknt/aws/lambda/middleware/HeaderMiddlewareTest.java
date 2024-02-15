package com.networknt.aws.lambda.middleware;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.aws.lambda.InvocationResponse;
import com.networknt.aws.lambda.LambdaContext;
import com.networknt.aws.lambda.middleware.chain.Chain;
import com.networknt.aws.lambda.middleware.chain.ChainDirection;
import com.networknt.aws.lambda.middleware.header.HeaderMiddleware;
import com.networknt.header.HeaderConfig;
import me.madhead.aws_junit5.common.AWSClient;
import me.madhead.aws_junit5.common.AWSEndpoint;
import me.madhead.aws_junit5.lambda.v2.Lambda;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.FunctionConfiguration;

import java.util.Collections;
import java.util.stream.Collectors;


@Testcontainers
class HeaderMiddlewareTest extends MiddlewareTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(TraceabilityMiddlewareTest.class);

    LightLambdaExchange exchange;

    DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:latest");

    @Container
    public LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.LAMBDA);

    @Test
    void test() {

        var client = LambdaClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .region(Region.of(localstack.getRegion()))
                .build();


        Assertions.assertNotNull(client);

        Assertions.assertEquals(
                Collections.emptyList(),
                client
                        .listFunctions()
                        .functions()
                        .stream()
                        .map(FunctionConfiguration::functionName)
                        .sorted()
                        .collect(Collectors.toList())
        );

//        APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent = null;
//        try {
//            apiGatewayProxyRequestEvent = OBJECT_MAPPER.readValue(testEvent, APIGatewayProxyRequestEvent.class);
//        } catch (JsonProcessingException e) {
//            LOG.error("Failed to read value as APIGatewayProxyRequestEvent");
//            throw new RuntimeException(e);
//        }
//        // add a request header so that it can be removed by the middleware
//        apiGatewayProxyRequestEvent.getHeaders().put("header1", "Header1Value");
//        apiGatewayProxyRequestEvent.getHeaders().put("header2", "Header2Value");
//        apiGatewayProxyRequestEvent.getHeaders().put("key1", "key1Old");
//        apiGatewayProxyRequestEvent.getHeaders().put("key2", "key2Old");
//
//        apiGatewayProxyRequestEvent.getHeaders().put("headerA", "HeaderAValue");
//        apiGatewayProxyRequestEvent.getHeaders().put("headerB", "HeaderAValue");
//        apiGatewayProxyRequestEvent.getHeaders().put("keyA", "keyAOld");
//        apiGatewayProxyRequestEvent.getHeaders().put("keyB", "keyBOld");
//
//        InvocationResponse invocation = InvocationResponse.builder()
//                .requestId("12345")
//                .event(apiGatewayProxyRequestEvent)
//                .build();
//        APIGatewayProxyRequestEvent requestEvent = invocation.getEvent();
//        Context lambdaContext = new LambdaContext(invocation.getRequestId());
//
//        Chain requestChain = new Chain(false, ChainDirection.REQUEST);
//        HeaderConfig headerConfig = HeaderConfig.load("header_test");
//        HeaderMiddleware headerMiddleware = new HeaderMiddleware(headerConfig);
//        requestChain.addChainable(headerMiddleware);
//        requestChain.setupGroupedChain();
//
//        this.exchange = new LightLambdaExchange(lambdaContext, requestChain, null);
//        this.exchange.setRequest(requestEvent);
//        exchange.executeRequestChain();
//        exchange.finalizeRequest();
//        requestEvent = exchange.getRequest();
//        // header1 and header2 should be removed from the request headers
//        assert(requestEvent.getHeaders().get("header1") == null);
//        assert(requestEvent.getHeaders().get("header2") == null);
//        // key1 and key2 should be updated in the request headers
//        assert(requestEvent.getHeaders().get("key1").equals("value1"));
//        assert(requestEvent.getHeaders().get("key2").equals("value2"));
//        // headerA and headerB should be removed from the request headers
//        assert(requestEvent.getHeaders().get("headerA") == null);
//        assert(requestEvent.getHeaders().get("headerB") == null);
//        // keyA and keyB should be updated in the request headers
//        assert(requestEvent.getHeaders().get("keyA").equals("valueA"));
//        assert(requestEvent.getHeaders().get("keyB").equals("valueB"));


    }

    public static class Endpoint implements AWSEndpoint {
        @Override
        public String url() {
            return System.getenv("LAMBDA_URL");
        }

        @Override
        public String region() {
            return System.getenv("LAMBDA_REGION");
        }

        @Override
        public String accessKey() {
            return System.getenv("LAMBDA_ACCESS_KEY");
        }

        @Override
        public String secretKey() {
            return System.getenv("LAMBDA_SECRET_KEY");
        }
    }
}
