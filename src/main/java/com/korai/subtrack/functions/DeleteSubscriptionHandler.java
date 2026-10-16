package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

import java.util.Map;

public class DeleteSubscriptionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;

    public DeleteSubscriptionHandler() {
        this(SharedRepositoryHolder.getInstance());
    }

    public DeleteSubscriptionHandler(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String id = (request != null && request.getPathParameters() != null)
                ? request.getPathParameters().get("id")
                : null;

        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error(400, "MISSING_ID", "삭제할 구독 ID가 지정되지 않았습니다.");
        }

        boolean deleted = repository.deleteById(id);
        if (!deleted) {
            return ApiResponse.error(404, "NOT_FOUND", "삭제할 구독 항목을 찾을 수 없습니다: " + id);
        }

        return ApiResponse.success(200, Map.of("deleted", true, "id", id));
    }
}
