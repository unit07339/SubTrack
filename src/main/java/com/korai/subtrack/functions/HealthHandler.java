package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.korai.subtrack.functions.common.ApiResponse;

import java.lang.management.ManagementFactory;
import java.util.Map;

public class HealthHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();

        Map<String, Object> healthInfo = Map.of(
                "status", "UP",
                "service", "SubTrack Serverless Web API",
                "version", "1.0.0",
                "runtime", "Java 21 (Serverless FaaS)",
                "uptimeMs", uptimeMs
        );

        return ApiResponse.success(200, healthInfo);
    }
}
