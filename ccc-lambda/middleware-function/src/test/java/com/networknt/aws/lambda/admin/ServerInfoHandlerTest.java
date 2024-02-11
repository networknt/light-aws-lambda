package com.networknt.aws.lambda.admin;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

public class ServerInfoHandlerTest {
    @Test
    public void testServerInfo() {
        ServerInfoHandler handler = new ServerInfoHandler();
        APIGatewayProxyResponseEvent responseEvent = handler.handleRequest(null, null);
        assert(responseEvent != null);
    }
}
