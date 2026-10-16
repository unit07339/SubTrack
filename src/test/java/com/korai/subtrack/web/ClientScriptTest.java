package com.korai.subtrack.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("클라이언트 스크립트 무결성 하네스 검증 테스트")
public class ClientScriptTest {

    @Test
    @DisplayName("api.js가 클래스패스에 존재하고 필수 API 호출 함수들을 포함하는지 검증")
    void testApiJsIntegrity() throws Exception {
        InputStream is = getClass().getResourceAsStream("/static/js/api.js");
        assertThat(is).isNotNull();

        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(content).contains("const SubTrackAPI =");
        assertThat(content).contains("getHealth:");
        assertThat(content).contains("getPresets:");
        assertThat(content).contains("getAnalyticsSummary:");
        assertThat(content).contains("getSubscriptions:");
        assertThat(content).contains("createSubscription:");
        assertThat(content).contains("deleteSubscription:");
        assertThat(content).contains("toggleStatus:");
    }

    @Test
    @DisplayName("app.js가 클래스패스에 존재하고 4대 뷰 렌더러 및 이벤트 핸들러를 포함하는지 검증")
    void testAppJsIntegrity() throws Exception {
        InputStream is = getClass().getResourceAsStream("/static/js/app.js");
        assertThat(is).isNotNull();

        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(content).contains("function switchTab");
        assertThat(content).contains("function renderDashboard");
        assertThat(content).contains("function renderSubscriptions");
        assertThat(content).contains("function renderCalendar");
        assertThat(content).contains("function renderAnalytics");
        assertThat(content).contains("function handleSubscriptionSubmit");
        assertThat(content).contains("function selectPreset");
    }
}
