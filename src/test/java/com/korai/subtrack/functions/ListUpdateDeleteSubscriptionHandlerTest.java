package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;
import com.korai.subtrack.core.repository.InMemorySubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("List, Update, Delete, ToggleStatus 서버리스 핸들러 검증 테스트")
public class ListUpdateDeleteSubscriptionHandlerTest {

    private InMemorySubscriptionRepository repository;
    private ListSubscriptionsHandler listHandler;
    private UpdateSubscriptionHandler updateHandler;
    private DeleteSubscriptionHandler deleteHandler;
    private ToggleStatusSubscriptionHandler toggleHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        repository.clear();
        listHandler = new ListSubscriptionsHandler(repository);
        updateHandler = new UpdateSubscriptionHandler(repository);
        deleteHandler = new DeleteSubscriptionHandler(repository);
        toggleHandler = new ToggleStatusSubscriptionHandler(repository);
    }

    @Test
    @DisplayName("전체 목록 및 상태/카테고리 필터링 조회 검증")
    void testListSubscriptionsWithFilters() throws Exception {
        Subscription s1 = repository.save(Subscription.builder().name("Netflix").cost(17000).billingDay(10).category(Category.ENTERTAINMENT).status(SubscriptionStatus.ACTIVE).build());
        Subscription s2 = repository.save(Subscription.builder().name("ChatGPT").cost(29000).billingDay(15).category(Category.PRODUCTIVITY).status(SubscriptionStatus.PAUSED).build());

        // 1. 전체 조회
        APIGatewayProxyResponseEvent respAll = listHandler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        assertThat(respAll.getStatusCode()).isEqualTo(200);
        JsonNode dataAll = mapper.readTree(respAll.getBody()).get("data");
        assertThat(dataAll.size()).isEqualTo(2);

        // 2. 상태 필터 (ACTIVE)
        APIGatewayProxyRequestEvent reqStatus = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(Map.of("status", "ACTIVE"));
        APIGatewayProxyResponseEvent respStatus = listHandler.handleRequest(reqStatus, null);
        JsonNode dataStatus = mapper.readTree(respStatus.getBody()).get("data");
        assertThat(dataStatus.size()).isEqualTo(1);
        assertThat(dataStatus.get(0).get("name").asText()).isEqualTo("Netflix");

        // 3. 잘못된 상태 필터 -> 400
        APIGatewayProxyRequestEvent reqInvalid = new APIGatewayProxyRequestEvent()
                .withQueryStringParameters(Map.of("status", "INVALID_VAL"));
        assertThat(listHandler.handleRequest(reqInvalid, null).getStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("구독 정보 수정(PUT) 및 미존재 ID 예외 검증")
    void testUpdateSubscription() throws Exception {
        Subscription s1 = repository.save(Subscription.builder().name("YouTube").cost(14900).billingDay(5).build());

        String updateBody = """
                {
                    "cost": 16900,
                    "memo": "가족 요금제 변경"
                }
                """;

        APIGatewayProxyRequestEvent reqUpdate = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("id", s1.getId()))
                .withBody(updateBody);

        APIGatewayProxyResponseEvent respUpdate = updateHandler.handleRequest(reqUpdate, null);
        assertThat(respUpdate.getStatusCode()).isEqualTo(200);

        JsonNode data = mapper.readTree(respUpdate.getBody()).get("data");
        assertThat(data.get("cost").asInt()).isEqualTo(16900);
        assertThat(data.get("memo").asText()).isEqualTo("가족 요금제 변경");

        // 미존재 ID 수정 -> 404
        APIGatewayProxyRequestEvent reqNotFound = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("id", "none"))
                .withBody(updateBody);
        assertThat(updateHandler.handleRequest(reqNotFound, null).getStatusCode()).isEqualTo(404);
    }

    @Test
    @DisplayName("구독 상태 토글(PATCH) 및 삭제(DELETE) 검증")
    void testToggleStatusAndDelete() throws Exception {
        Subscription s1 = repository.save(Subscription.builder().name("Disney+").cost(9900).billingDay(20).status(SubscriptionStatus.ACTIVE).build());

        // 1. 상태 일시정지로 변경
        APIGatewayProxyRequestEvent reqToggle = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("id", s1.getId()))
                .withBody("{\"status\": \"PAUSED\"}");

        APIGatewayProxyResponseEvent respToggle = toggleHandler.handleRequest(reqToggle, null);
        assertThat(respToggle.getStatusCode()).isEqualTo(200);
        JsonNode toggleData = mapper.readTree(respToggle.getBody()).get("data");
        assertThat(toggleData.get("status").asText()).isEqualTo("PAUSED");

        // 2. 삭제 실행
        APIGatewayProxyRequestEvent reqDelete = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("id", s1.getId()));
        APIGatewayProxyResponseEvent respDelete = deleteHandler.handleRequest(reqDelete, null);
        assertThat(respDelete.getStatusCode()).isEqualTo(200);

        // 3. 재삭제 시 404
        APIGatewayProxyResponseEvent respDeleteAgain = deleteHandler.handleRequest(reqDelete, null);
        assertThat(respDeleteAgain.getStatusCode()).isEqualTo(404);
    }
}
