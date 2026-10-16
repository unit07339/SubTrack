package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.korai.subtrack.core.calculator.BillingCalculator;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

import java.time.LocalDate;
import java.util.Optional;

public class UpdateSubscriptionHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;
    private final ObjectMapper mapper;

    public UpdateSubscriptionHandler() {
        this(SharedRepositoryHolder.getInstance());
    }

    public UpdateSubscriptionHandler(SubscriptionRepository repository) {
        this.repository = repository;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String id = (request != null && request.getPathParameters() != null)
                ? request.getPathParameters().get("id")
                : null;

        if (id == null || id.trim().isEmpty()) {
            return ApiResponse.error(400, "MISSING_ID", "수정할 구독 ID가 지정되지 않았습니다.");
        }

        Optional<Subscription> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            return ApiResponse.error(404, "NOT_FOUND", "해당 ID의 구독 항목을 찾을 수 없습니다: " + id);
        }

        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            return ApiResponse.error(400, "BAD_REQUEST", "수정할 요청 본문이 비어 있습니다.");
        }

        try {
            Subscription input = mapper.readValue(request.getBody(), Subscription.class);
            Subscription existing = existingOpt.get();

            if (input.getName() != null && !input.getName().trim().isEmpty()) {
                existing.setName(input.getName());
            }
            if (input.getCost() >= 0) {
                existing.setCost(input.getCost());
            }
            if (input.getBillingDay() >= 1 && input.getBillingDay() <= 31) {
                existing.setBillingDay(input.getBillingDay());
            }
            if (input.getBillingCycle() != null) {
                existing.setBillingCycle(input.getBillingCycle());
            }
            if (input.getCategory() != null) {
                existing.setCategory(input.getCategory());
            }
            if (input.getPaymentMethod() != null) {
                existing.setPaymentMethod(input.getPaymentMethod());
            }
            if (input.getStatus() != null) {
                existing.setStatus(input.getStatus());
            }
            if (input.getIconKey() != null) {
                existing.setIconKey(input.getIconKey());
            }
            if (input.getMemo() != null) {
                existing.setMemo(input.getMemo());
            }

            // 결제일 재계산
            LocalDate nextDate = BillingCalculator.calculateNextBillingDate(
                    LocalDate.now(),
                    existing.getBillingDay(),
                    existing.getBillingCycle()
            );
            existing.setNextBillingDate(nextDate);

            Subscription updated = repository.save(existing);
            return ApiResponse.success(200, updated);
        } catch (Exception e) {
            return ApiResponse.error(400, "INVALID_INPUT", "수정 데이터 파싱 실패: " + e.getMessage());
        }
    }
}
