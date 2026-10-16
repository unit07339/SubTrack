package com.korai.subtrack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("하네스 베이스라인 검증 테스트")
public class HarnessBaselineTest {

    @Test
    @DisplayName("JUnit 5 및 AssertJ 기본 동작 검증")
    void testHarnessAssertions() {
        String status = "ACTIVE";
        assertThat(status).isNotNull().isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Jackson JSON 매퍼 및 Java 8 날짜 시간 모듈 직렬화 검증")
    void testJacksonSerialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        LocalDate today = LocalDate.of(2026, 10, 16);
        Map<String, Object> payload = Map.of(
                "service", "Netflix",
                "billingDate", today.toString(),
                "cost", 17000
        );

        String json = mapper.writeValueAsString(payload);
        assertThat(json).contains("Netflix").contains("17000").contains("2026-10-16");

        Map<?, ?> deserialized = mapper.readValue(json, Map.class);
        assertThat(deserialized.get("service")).isEqualTo("Netflix");
        assertThat(deserialized.get("cost")).isEqualTo(17000);
    }
}
