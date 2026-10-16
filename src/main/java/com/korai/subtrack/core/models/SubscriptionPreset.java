package com.korai.subtrack.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPreset {
    private String key;
    private String name;
    private int defaultCost;
    private Category category;
    private BillingCycle defaultBillingCycle;
    private String colorHex;
    private String description;

    public static List<SubscriptionPreset> getStandardPresets() {
        return List.of(
                new SubscriptionPreset("netflix", "넷플릭스 (Netflix)", 17000, Category.ENTERTAINMENT, BillingCycle.MONTHLY, "#E50914", "스탠다드/프리미엄 요금제"),
                new SubscriptionPreset("youtube", "유튜브 프리미엄 (YouTube)", 14900, Category.ENTERTAINMENT, BillingCycle.MONTHLY, "#FF0000", "광고 없는 영상 및 백그라운드 재생"),
                new SubscriptionPreset("disney", "디즈니+ (Disney+)", 9900, Category.ENTERTAINMENT, BillingCycle.MONTHLY, "#113CCF", "디즈니, 마블, 스타워즈"),
                new SubscriptionPreset("spotify", "스포티파이 (Spotify)", 10900, Category.MUSIC, BillingCycle.MONTHLY, "#1DB954", "전 세계 음원 스트리밍"),
                new SubscriptionPreset("chatgpt", "ChatGPT Plus (OpenAI)", 29000, Category.PRODUCTIVITY, BillingCycle.MONTHLY, "#10A37F", "GPT-4o 및 고급 AI 도구"),
                new SubscriptionPreset("coupang", "쿠팡 와우 멤버십", 7890, Category.LIFESTYLE, BillingCycle.MONTHLY, "#0073E6", "무료 로켓배송 및 쿠팡플레이"),
                new SubscriptionPreset("naver", "네이버플러스 멤버십", 4900, Category.LIFESTYLE, BillingCycle.MONTHLY, "#03C75A", "네이버 쇼핑 최대 5% 적립 및 콘텐츠"),
                new SubscriptionPreset("github", "GitHub Copilot", 13000, Category.PRODUCTIVITY, BillingCycle.MONTHLY, "#24292E", "AI 페어 프로그래밍 도구")
        );
    }
}
