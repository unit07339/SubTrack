package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

import java.util.List;
import java.util.Map;

public class ListSubscriptionsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;

    public ListSubscriptionsHandler() {
        this(SharedRepositoryHolder.getInstance());
    }

    public ListSubscriptionsHandler(SubscriptionRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> queryParams = (request != null && request.getQueryStringParameters() != null)
                ? request.getQueryStringParameters()
                : Map.of();

        String statusParam = queryParams.get("status");
        String categoryParam = queryParams.get("category");

        List<Subscription> results;

        if (statusParam != null && !statusParam.trim().isEmpty()) {
            try {
                SubscriptionStatus status = SubscriptionStatus.valueOf(statusParam.toUpperCase());
                results = repository.findByStatus(status);
            } catch (IllegalArgumentException e) {
                return ApiResponse.error(400, "INVALID_STATUS", "유효하지 않은 status 값입니다: " + statusParam);
            }
        } else if (categoryParam != null && !categoryParam.trim().isEmpty()) {
            try {
                Category category = Category.valueOf(categoryParam.toUpperCase());
                results = repository.findByCategory(category);
            } catch (IllegalArgumentException e) {
                return ApiResponse.error(400, "INVALID_CATEGORY", "유효하지 않은 category 값입니다: " + categoryParam);
            }
        } else {
            results = repository.findAll();
        }

        return ApiResponse.success(200, results);
    }
}
