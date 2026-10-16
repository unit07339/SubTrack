package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HealthHandler 및 PresetsHandler 서버리스 검증 테스트")
public class HealthAndPresetsHandlerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("HealthHandler HTTP 200 반환 및 Envelope 응답 구조 검증")
    void testHealthHandler() throws Exception {
        HealthHandler handler = new HealthHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(response.getHeaders()).containsEntry("Content-Type", "application/json; charset=UTF-8");
        assertThat(response.getHeaders()).containsEntry("Access-Control-Allow-Origin", "*");

        JsonNode root = mapper.readTree(response.getBody());
        assertThat(root.get("success").asBoolean()).isTrue();
        assertThat(root.get("timestamp")).isNotNull();
        assertThat(root.get("data").get("status").asText()).isEqualTo("UP");
        assertThat(root.get("data").get("service").asText()).contains("SubTrack");
    }

    @Test
    @DisplayName("PresetsHandler HTTP 200 반환 및 프리셋 배열 데이터 검증")
    void testPresetsHandler() throws Exception {
        PresetsHandler handler = new PresetsHandler();
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent();

        APIGatewayProxyResponseEvent response = handler.handleRequest(request, null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode root = mapper.readTree(response.getBody());
        assertThat(root.get("success").asBoolean()).isTrue();

        JsonNode data = root.get("data");
        assertThat(data.isArray()).isTrue();
        assertThat(data.size()).isGreaterThanOrEqualTo(5);

        // 넷플릭스 프리셋 포함 여부 확인
        boolean containsNetflix = false;
        for (JsonNode item : data) {
            if ("netflix".equals(item.get("key").asText())) {
                containsNetflix = true;
                assertThat(item.get("defaultCost").asInt()).isEqualTo(17000);
                break;
            }
        }
        assertThat(containsNetflix).isTrue();
    }
}
