package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.repository.InMemorySubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Create 및 Get Subscription 서버리스 핸들러 검증 테스트")
public class CreateAndGetSubscriptionHandlerTest {

    private InMemorySubscriptionRepository repository;
    private CreateSubscriptionHandler createHandler;
    private GetSubscriptionHandler getHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        repository.clear();
        createHandler = new CreateSubscriptionHandler(repository);
        getHandler = new GetSubscriptionHandler(repository);
    }

    @Test
    @DisplayName("신규 구독 정상 등록 시 HTTP 201 및 다음 결제일 자동 계산 반환")
    void testCreateSubscriptionSuccess() throws Exception {
        String body = """
                {
                    "name": "Netflix",
                    "cost": 17000,
                    "billingDay": 25,
                    "category": "ENTERTAINMENT"
                }
                """;

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody(body);
        APIGatewayProxyResponseEvent response = createHandler.handleRequest(request, null);

        assertThat(response.getStatusCode()).isEqualTo(201);

        JsonNode root = mapper.readTree(response.getBody());
        assertThat(root.get("success").asBoolean()).isTrue();

        JsonNode data = root.get("data");
        assertThat(data.get("id").asText()).isNotBlank();
        assertThat(data.get("name").asText()).isEqualTo("Netflix");
        assertThat(data.get("cost").asInt()).isEqualTo(17000);
        assertThat(data.get("nextBillingDate").asText()).isNotBlank();
    }

    @Test
    @DisplayName("잘못된 입력값(이름 누락 또는 비정상 일자) 시 HTTP 400 반환")
    void testCreateSubscriptionValidationFailure() throws Exception {
        String invalidBody = """
                {
                    "name": "   ",
                    "cost": 10000,
                    "billingDay": 40
                }
                """;

        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent().withBody(invalidBody);
        APIGatewayProxyResponseEvent response = createHandler.handleRequest(request, null);

        assertThat(response.getStatusCode()).isEqualTo(400);

        JsonNode root = mapper.readTree(response.getBody());
        assertThat(root.get("success").asBoolean()).isFalse();
        assertThat(root.get("error").get("code").asText()).isEqualTo("INVALID_INPUT");
    }

    @Test
    @DisplayName("빈 본문 또는 잘못된 JSON 요청 시 HTTP 400 반환")
    void testCreateSubscriptionMalformedJson() throws Exception {
        APIGatewayProxyRequestEvent requestEmpty = new APIGatewayProxyRequestEvent().withBody("");
        assertThat(createHandler.handleRequest(requestEmpty, null).getStatusCode()).isEqualTo(400);

        APIGatewayProxyRequestEvent requestMalformed = new APIGatewayProxyRequestEvent().withBody("{ invalid-json }");
        assertThat(createHandler.handleRequest(requestMalformed, null).getStatusCode()).isEqualTo(400);
    }

    @Test
    @DisplayName("등록된 구독 ID로 단건 조회 성공 및 미존재 시 HTTP 404 반환")
    void testGetSubscriptionById() throws Exception {
        Subscription sub = Subscription.builder()
                .name("Spotify")
                .cost(10900)
                .billingDay(15)
                .category(Category.MUSIC)
                .build();
        Subscription saved = repository.save(sub);

        // 1. 존재하는 ID 조회 (성공)
        APIGatewayProxyRequestEvent request = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("id", saved.getId()));
        APIGatewayProxyResponseEvent response = getHandler.handleRequest(request, null);

        assertThat(response.getStatusCode()).isEqualTo(200);
        JsonNode root = mapper.readTree(response.getBody());
        assertThat(root.get("data").get("name").asText()).isEqualTo("Spotify");

        // 2. 존재하지 않는 ID 조회 (404)
        APIGatewayProxyRequestEvent notFoundRequest = new APIGatewayProxyRequestEvent()
                .withPathParameters(Map.of("id", "unknown-id"));
        APIGatewayProxyResponseEvent notFoundResponse = getHandler.handleRequest(notFoundRequest, null);

        assertThat(notFoundResponse.getStatusCode()).isEqualTo(404);
    }
}
