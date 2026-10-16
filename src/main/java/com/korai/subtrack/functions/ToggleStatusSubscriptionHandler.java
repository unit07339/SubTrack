package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

import java.util.NoSuchElementException;

public class ToggleStatusSubscriptionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public ToggleStatusSubscriptionHandler() {
        this(SharedRepositoryHolder.getInstance());
    }

    public ToggleStatusSubscriptionHandler(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String id = (request != null && request.getPathParameters() != null)
                ? request.getPathParameters().get("id")
                : null;

        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error(400, "MISSING_ID", "상태를 변경할 구독 ID가 지정되지 않았습니다.");
        }

        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            return ApiResponse.error(400, "BAD_REQUEST", "변경할 상태 본문이 비어 있습니다.");
        }

        try {
            JsonNode bodyNode = mapper.readTree(request.getBody());
            if (!bodyNode.has("status")) {
                return ApiResponse.error(400, "MISSING_STATUS", "status 필드가 누락되었습니다.");
            }

            SubscriptionStatus newStatus = SubscriptionStatus.valueOf(bodyNode.get("status").asText().toUpperCase());
            Subscription updated = repository.updateStatus(id, newStatus);

            return ApiResponse.success(200, updated);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, "INVALID_STATUS", "유효하지 않은 status 값입니다.");
        } catch (NoSuchElementException e) {
            return ApiResponse.error(404, "NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(400, "INVALID_JSON", "요청 파싱 실패: " + e.getMessage());
        }
    }
}
