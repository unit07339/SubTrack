package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

import java.util.Optional;

public class GetSubscriptionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;

    public GetSubscriptionHandler() {
        this(SharedRepositoryHolder.getInstance());
    }

    public GetSubscriptionHandler(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String id = null;

        if (request != null && request.getPathParameters() != null) {
            id = request.getPathParameters().get("id");
        }
        if (id == null && request != null && request.getQueryStringParameters() != null) {
            id = request.getQueryStringParameters().get("id");
        }

        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error(400, "MISSING_ID", "조회할 구독 ID가 지정되지 않았습니다.");
        }

        Optional<Subscription> found = repository.findById(id);
        if (found.isEmpty()) {
            return ApiResponse.error(404, "NOT_FOUND", "해당 ID의 구독 항목을 찾을 수 없습니다: " + id);
        }

        return ApiResponse.success(200, found.get());
    }
}
