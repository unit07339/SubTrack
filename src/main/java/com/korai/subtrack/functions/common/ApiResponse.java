package com.korai.subtrack.functions.common;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

public class ApiResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final Map<String, String> CORS_HEADERS = Map.of(
            "Content-Type", "application/json; charset=UTF-8",
            "Access-Control-Allow-Origin", "*",
            "Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With"
    );

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseEnvelope<T> {
        private boolean success;
        private T data;
        private ErrorDetail error;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private String code;
        private String message;
    }

    public static <T> APIGatewayProxyResponseEvent success(int statusCode, T data) {
        try {
            ResponseEnvelope<T> envelope = ResponseEnvelope.<T>builder()
                    .success(true)
                    .data(data)
                    .timestamp(Instant.now())
                    .build();

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(CORS_HEADERS)
                    .withBody(MAPPER.writeValueAsString(envelope));
        } catch (Exception e) {
            return error(500, "INTERNAL_SERVER_ERROR", "응답 직렬화 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    public static APIGatewayProxyResponseEvent error(int statusCode, String errorCode, String message) {
        try {
            ResponseEnvelope<Void> envelope = ResponseEnvelope.<Void>builder()
                    .success(false)
                    .error(new ErrorDetail(errorCode, message))
                    .timestamp(Instant.now())
                    .build();

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(statusCode)
                    .withHeaders(CORS_HEADERS)
                    .withBody(MAPPER.writeValueAsString(envelope));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(CORS_HEADERS)
                    .withBody("{\"success\":false,\"error\":{\"code\":\"FATAL\",\"message\":\"Internal Error\"}}");
        }
    }
}
