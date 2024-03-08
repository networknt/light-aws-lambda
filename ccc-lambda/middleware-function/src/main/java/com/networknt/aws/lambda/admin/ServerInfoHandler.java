package com.networknt.aws.lambda.admin;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.networknt.aws.lambda.handler.LambdaHandler;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.info.ServerInfoConfig;
import com.networknt.info.ServerInfoUtil;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class ServerInfoHandler implements LambdaHandler {
    static final String STATUS_SERVER_INFO_DISABLED = "ERR10013";
    static final Logger logger = LoggerFactory.getLogger(ServerInfoHandler.class);
    static ServerInfoConfig config;

    public ServerInfoHandler() {
        logger.info("ServerInfoHandler is constructed");
        config = ServerInfoConfig.load();
        ModuleRegistry.registerModule(ServerInfoConfig.CONFIG_NAME, ServerInfoHandler.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ServerInfoConfig.CONFIG_NAME),null);
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent apiGatewayProxyRequestEvent, com.amazonaws.services.lambda.runtime.Context context) {
        Map<String, String> headers = Map.of("Content-Type", "application/json");
        if(config.isEnableServerInfo()) {
            Map<String,Object> infoMap = ServerInfoUtil.getServerInfo(config);
            // TODO access the downstream to get the server info from the downstream
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withHeaders(headers).withBody(JsonMapper.toJson((infoMap)));
        } else {
            Status status = new Status(STATUS_SERVER_INFO_DISABLED);
            return new APIGatewayProxyResponseEvent().withStatusCode(status.getStatusCode()).withHeaders(headers).withBody(status.toString());
        }
    }

}
