package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.repository.InMemorySubscriptionRepository;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

public class CreateSubscriptionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;
    private final ObjectMapper mapper;

    public CreateSubscriptionHandler() {
        this(SharedRepositoryHolder.getInstance());
    }

    public CreateSubscriptionHandler(SubscriptionRepository repository) {
        this.repository = repository;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        if (request == null || request.getBody() == null || request.getBody().trim().isEmpty()) {
            return ApiResponse.error(400, "BAD_REQUEST", "요청 본문(body)이 비어 있습니다.");
        }

        try {
            Subscription subscription = mapper.readValue(request.getBody(), Subscription.class);
            subscription.validate();

            Subscription saved = repository.save(subscription);
            return ApiResponse.success(201, saved);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(400, "INVALID_INPUT", e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error(400, "INVALID_JSON", "JSON 파싱 실패: " + e.getMessage());
        }
    }
}
