package com.korai.subtrack.core.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Subscription 도메인 모델 검증 테스트")
public class SubscriptionTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("유효한 구독 객체 정상 생성 및 기본값 채우기 검증")
    void testValidSubscriptionCreation() {
        Subscription sub = Subscription.builder()
                .name("Netflix")
                .cost(17000)
                .billingDay(25)
                .nextBillingDate(LocalDate.of(2026, 10, 25))
                .build();

        sub.validate();

        assertThat(sub.getId()).isNotBlank();
        assertThat(sub.getCurrency()).isEqualTo("KRW");
        assertThat(sub.getBillingCycle()).isEqualTo(BillingCycle.MONTHLY);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getCategory()).isEqualTo(Category.OTHER);
        assertThat(sub.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이름 누락 또는 비어있을 시 예외 발생 검증")
    void testInvalidNameThrows() {
        Subscription sub = Subscription.builder()
                .name("   ")
                .cost(10000)
                .billingDay(15)
                .build();

        assertThatThrownBy(sub::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("서비스명");
    }

    @Test
    @DisplayName("결제일 1~31 범위 벗어날 시 예외 발생 검증")
    void testInvalidBillingDayThrows() {
        Subscription sub = Subscription.builder()
                .name("Spotify")
                .cost(10900)
                .billingDay(32)
                .build();

        assertThatThrownBy(sub::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제일");
    }

    @Test
    @DisplayName("표준 프리셋 목록 제공 및 데이터 정합성 검증")
    void testStandardPresets() {
        List<SubscriptionPreset> presets = SubscriptionPreset.getStandardPresets();
        assertThat(presets).isNotEmpty().hasSizeGreaterThanOrEqualTo(5);
        assertThat(presets).anyMatch(p -> p.getKey().equals("netflix") && p.getDefaultCost() == 17000);
        assertThat(presets).anyMatch(p -> p.getKey().equals("youtube") && p.getCategory() == Category.ENTERTAINMENT);
    }

    @Test
    @DisplayName("Subscription 객체의 Jackson JSON 직렬화 및 역직렬화 왕복 검증")
    void testJsonRoundTrip() throws Exception {
        Subscription sub = Subscription.builder()
                .id("sub-123")
                .name("ChatGPT Plus")
                .cost(29000)
                .currency("KRW")
                .billingCycle(BillingCycle.MONTHLY)
                .billingDay(10)
                .nextBillingDate(LocalDate.of(2026, 11, 10))
                .category(Category.PRODUCTIVITY)
                .status(SubscriptionStatus.ACTIVE)
                .iconKey("chatgpt")
                .memo("업무용 AI")
                .build();

        sub.validate();

        String json = objectMapper.writeValueAsString(sub);
        assertThat(json).contains("sub-123")
                .contains("ChatGPT Plus")
                .contains("2026-11-10")
                .contains("PRODUCTIVITY");

        Subscription deserialized = objectMapper.readValue(json, Subscription.class);
        assertThat(deserialized.getId()).isEqualTo("sub-123");
        assertThat(deserialized.getName()).isEqualTo("ChatGPT Plus");
        assertThat(deserialized.getCategory()).isEqualTo(Category.PRODUCTIVITY);
        assertThat(deserialized.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 11, 10));
    }
}
