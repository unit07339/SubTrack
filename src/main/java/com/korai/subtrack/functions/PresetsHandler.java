package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.korai.subtrack.core.models.SubscriptionPreset;
import com.korai.subtrack.functions.common.ApiResponse;

import java.util.List;

public class PresetsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        List<SubscriptionPreset> presets = SubscriptionPreset.getStandardPresets();
        return ApiResponse.success(200, presets);
    }
}
