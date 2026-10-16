package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.korai.subtrack.core.models.BillingCycle;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;
import com.korai.subtrack.core.repository.InMemorySubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnalyticsSummaryHandler 대시보드 지출 통계 핸들러 검증 테스트")
public class AnalyticsSummaryHandlerTest {

    private InMemorySubscriptionRepository repository;
    private AnalyticsSummaryHandler handler;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        repository.clear();
        handler = new AnalyticsSummaryHandler(repository, fixedClock);
    }

    @Test
    @DisplayName("구독 데이터가 없을 때 기본 0원 및 빈 배열 정상 반환")
    void testEmptyAnalytics() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JsonNode data = mapper.readTree(response.getBody()).get("data");
        assertThat(data.get("monthlyTotal").asInt()).isEqualTo(0);
        assertThat(data.get("activeCount").asInt()).isEqualTo(0);
        assertThat(data.get("upcomingSubscriptions").size()).isEqualTo(0);
    }

    @Test
    @DisplayName("다중 구독에 대한 이번 달 총액, 연간 환산액, D-7 임박 알림, 카테고리 비중 계산 검증")
    void testAnalyticsCalculations() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 16);

        // 1. Netflix: 17,000원 / 월간 / 10월 19일 (D-3일 남음)
        repository.save(Subscription.builder()
                .name("Netflix")
                .cost(17000)
                .billingCycle(BillingCycle.MONTHLY)
                .billingDay(19)
                .nextBillingDate(today.plusDays(3))
                .category(Category.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build());

        // 2. Spotify: 120,000원 / 연간 (월 환산 10,000원) / 10월 16일 (오늘 결제 D-0)
        repository.save(Subscription.builder()
                .name("Spotify")
                .cost(120000)
                .billingCycle(BillingCycle.YEARLY)
                .billingDay(16)
                .nextBillingDate(today)
                .category(Category.MUSIC)
                .status(SubscriptionStatus.ACTIVE)
                .build());

        // 3. ChatGPT Plus: 29,000원 / 월간 / 11월 5일 (D-20일 남음, D-7 제외)
        repository.save(Subscription.builder()
                .name("ChatGPT Plus")
                .cost(29000)
                .billingCycle(BillingCycle.MONTHLY)
                .billingDay(5)
                .nextBillingDate(today.plusDays(20))
                .category(Category.PRODUCTIVITY)
                .status(SubscriptionStatus.ACTIVE)
                .build());

        // 4. Coupang Wow: 일시정지(PAUSED) 상태 -> 총액 계산에서 제외되어야 함
        repository.save(Subscription.builder()
                .name("Coupang Wow")
                .cost(7890)
                .billingCycle(BillingCycle.MONTHLY)
                .billingDay(17)
                .nextBillingDate(today.plusDays(1))
                .category(Category.LIFESTYLE)
                .status(SubscriptionStatus.PAUSED)
                .build());

        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), null);
        assertThat(response.getStatusCode()).isEqualTo(200);

        JsonNode data = mapper.readTree(response.getBody()).get("data");

        // 월간 총액: 17000 + 10000 + 29000 = 56000원
        assertThat(data.get("monthlyTotal").asInt()).isEqualTo(56000);

        // 연간 총액: (17000*12) + 120000 + (29000*12) = 204000 + 120000 + 348000 = 672000원
        assertThat(data.get("yearlyProjectedTotal").asInt()).isEqualTo(672000);

        // 카운트: 활성 3건, 일시정지 1건
        assertThat(data.get("activeCount").asInt()).isEqualTo(3);
        assertThat(data.get("pausedCount").asInt()).isEqualTo(1);

        // D-7 이내 임박 건수: Spotify(0일 남음)과 Netflix(3일 남음) 총 2건
        JsonNode upcoming = data.get("upcomingSubscriptions");
        assertThat(upcoming.size()).isEqualTo(2);
        assertThat(upcoming.get(0).get("subscription").get("name").asText()).isEqualTo("Spotify");
        assertThat(upcoming.get(0).get("daysLeft").asLong()).isEqualTo(0);
        assertThat(upcoming.get(1).get("subscription").get("name").asText()).isEqualTo("Netflix");
        assertThat(upcoming.get(1).get("daysLeft").asLong()).isEqualTo(3);

        // 가장 비싼 구독 1위: ChatGPT Plus (29,000원)
        JsonNode top = data.get("topExpensive");
        assertThat(top.get(0).get("name").asText()).isEqualTo("ChatGPT Plus");
    }
}
