package com.korai.subtrack.e2e;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korai.subtrack.core.repository.InMemorySubscriptionRepository;
import com.korai.subtrack.functions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SubTrack 전체 E2E 통합 라이프사이클 계약 검증 테스트")
public class E2EContractTest {

    private InMemorySubscriptionRepository repository;
    private HealthHandler healthHandler;
    private PresetsHandler presetsHandler;
    private CreateSubscriptionHandler createHandler;
    private GetSubscriptionHandler getHandler;
    private ListSubscriptionsHandler listHandler;
    private UpdateSubscriptionHandler updateHandler;
    private ToggleStatusSubscriptionHandler toggleHandler;
    private DeleteSubscriptionHandler deleteHandler;
    private AnalyticsSummaryHandler analyticsHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        repository.clear();

        healthHandler = new HealthHandler();
        presetsHandler = new PresetsHandler();
        createHandler = new CreateSubscriptionHandler(repository);
        getHandler = new GetSubscriptionHandler(repository);
        listHandler = new ListSubscriptionsHandler(repository);
        updateHandler = new UpdateSubscriptionHandler(repository);
        toggleHandler = new ToggleStatusSubscriptionHandler(repository);
        deleteHandler = new DeleteSubscriptionHandler(repository);
        analyticsHandler = new AnalyticsSummaryHandler(repository);
    }

    @Test
    @DisplayName("구독 라이프사이클 전 과정(헬스체크->프리셋조회->등록->목록->통계->상태변경->수정->삭제) E2E 검증")
    void testCompleteUserLifecycle() throws Exception {
        // [Step 1] 헬스체크
        APIGatewayProxyResponseEvent healthResp = healthHandler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        assertThat(healthResp.getStatusCode()).isEqualTo(200);
        assertThat(mapper.readTree(healthResp.getBody()).get("data").get("status").asText()).isEqualTo("UP");

        // [Step 2] 프리셋 조회
        APIGatewayProxyResponseEvent presetResp = presetsHandler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        assertThat(presetResp.getStatusCode()).isEqualTo(200);
        JsonNode presets = mapper.readTree(presetResp.getBody()).get("data");
        assertThat(presets.size()).isGreaterThanOrEqualTo(5);

        // [Step 3] 넷플릭스 신규 등록 (17,000원 / 월간)
        String netflixBody = """
                {
                    "name": "넷플릭스",
                    "cost": 17000,
                    "billingCycle": "MONTHLY",
                    "billingDay": 25,
                    "category": "ENTERTAINMENT",
                    "paymentMethod": "신한카드"
                }
                """;
        APIGatewayProxyResponseEvent netflixResp = createHandler.handleRequest(
                new APIGatewayProxyRequestEvent().withBody(netflixBody), null
        );
        assertThat(netflixResp.getStatusCode()).isEqualTo(201);
        String netflixId = mapper.readTree(netflixResp.getBody()).get("data").get("id").asText();
        assertThat(netflixId).isNotBlank();

        // [Step 4] ChatGPT Plus 신규 등록 (29,000원 / 월간)
        String gptBody = """
                {
                    "name": "ChatGPT Plus",
                    "cost": 29000,
                    "billingCycle": "MONTHLY",
                    "billingDay": 10,
                    "category": "PRODUCTIVITY",
                    "paymentMethod": "현대카드"
                }
                """;
        APIGatewayProxyResponseEvent gptResp = createHandler.handleRequest(
                new APIGatewayProxyRequestEvent().withBody(gptBody), null
        );
        assertThat(gptResp.getStatusCode()).isEqualTo(201);
        String gptId = mapper.readTree(gptResp.getBody()).get("data").get("id").asText();

        // [Step 5] 전체 목록 조회
        APIGatewayProxyResponseEvent listResp = listHandler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        assertThat(listResp.getStatusCode()).isEqualTo(200);
        JsonNode listData = mapper.readTree(listResp.getBody()).get("data");
        assertThat(listData.size()).isEqualTo(2);

        // [Step 6] 대시보드 통계 조회 (총액 17,000 + 29,000 = 46,000원)
        APIGatewayProxyResponseEvent statsResp1 = analyticsHandler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        JsonNode statsData1 = mapper.readTree(statsResp1.getBody()).get("data");
        assertThat(statsData1.get("monthlyTotal").asInt()).isEqualTo(46000);
        assertThat(statsData1.get("activeCount").asInt()).isEqualTo(2);

        // [Step 7] 넷플릭스 일시정지(PAUSED)로 전환
        APIGatewayProxyResponseEvent toggleResp = toggleHandler.handleRequest(
                new APIGatewayProxyRequestEvent()
                        .withPathParameters(Map.of("id", netflixId))
                        .withBody("{\"status\": \"PAUSED\"}"),
                null
        );
        assertThat(toggleResp.getStatusCode()).isEqualTo(200);

        // [Step 8] 통계 재조회 (일시정지 제외로 월 29,000원, 활성 1개, 일시정지 1개)
        APIGatewayProxyResponseEvent statsResp2 = analyticsHandler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        JsonNode statsData2 = mapper.readTree(statsResp2.getBody()).get("data");
        assertThat(statsData2.get("monthlyTotal").asInt()).isEqualTo(29000);
        assertThat(statsData2.get("activeCount").asInt()).isEqualTo(1);
        assertThat(statsData2.get("pausedCount").asInt()).isEqualTo(1);

        // [Step 9] ChatGPT 가격 수정 (29,000원 -> 32,000원)
        APIGatewayProxyResponseEvent updateResp = updateHandler.handleRequest(
                new APIGatewayProxyRequestEvent()
                        .withPathParameters(Map.of("id", gptId))
                        .withBody("{\"cost\": 32000}"),
                null
        );
        assertThat(updateResp.getStatusCode()).isEqualTo(200);
        assertThat(mapper.readTree(updateResp.getBody()).get("data").get("cost").asInt()).isEqualTo(32000);

        // [Step 10] 넷플릭스 영구 삭제
        APIGatewayProxyResponseEvent deleteResp = deleteHandler.handleRequest(
                new APIGatewayProxyRequestEvent().withPathParameters(Map.of("id", netflixId)),
                null
        );
        assertThat(deleteResp.getStatusCode()).isEqualTo(200);

        // 삭제 후 단건 조회 시 404 확인
        APIGatewayProxyResponseEvent getDeletedResp = getHandler.handleRequest(
                new APIGatewayProxyRequestEvent().withPathParameters(Map.of("id", netflixId)),
                null
        );
        assertThat(getDeletedResp.getStatusCode()).isEqualTo(404);
    }
}
