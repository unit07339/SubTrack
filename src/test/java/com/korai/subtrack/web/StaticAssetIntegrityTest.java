package com.korai.subtrack.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("정적 웹 에셋 무결성 검증 하네스 테스트")
public class StaticAssetIntegrityTest {

    @Test
    @DisplayName("index.html이 클래스패스에 존재하고 필수 SPA 뷰 및 메타 태그를 포함하는지 검증")
    void testIndexHtmlIntegrity() throws Exception {
        InputStream is = getClass().getResourceAsStream("/static/index.html");
        assertThat(is).isNotNull();

        String html = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        // 1. 반응형 뷰포트 메타태그
        assertThat(html).contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");

        // 2. 4대 핵심 뷰 섹션 마크업
        assertThat(html).contains("id=\"view-dashboard\"");
        assertThat(html).contains("id=\"view-subscriptions\"");
        assertThat(html).contains("id=\"view-calendar\"");
        assertThat(html).contains("id=\"view-analytics\"");

        // 3. 네비게이션 탭 마크업
        assertThat(html).contains("id=\"tab-dashboard\"");
        assertThat(html).contains("id=\"tab-subscriptions\"");
        assertThat(html).contains("id=\"tab-calendar\"");
        assertThat(html).contains("id=\"tab-analytics\"");

        // 4. 모달 및 핵심 컴포넌트
        assertThat(html).contains("id=\"modal-subscription\"");
        assertThat(html).contains("id=\"form-subscription\"");
        assertThat(html).contains("id=\"preset-buttons\"");

        // 5. CSS 및 JS 연결
        assertThat(html).contains("href=\"css/style.css\"");
        assertThat(html).contains("src=\"js/api.js\"");
        assertThat(html).contains("src=\"js/app.js\"");
    }

    @Test
    @DisplayName("css/style.css가 클래스패스에 존재하고 커스텀 클래스가 정의되어 있는지 검증")
    void testStyleCssIntegrity() throws Exception {
        InputStream is = getClass().getResourceAsStream("/static/css/style.css");
        assertThat(is).isNotNull();

        String css = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertThat(css).contains(".glass-card");
        assertThat(css).contains(".nav-tab.active");
        assertThat(css).contains(".badge-dday-imminent");
    }
}
