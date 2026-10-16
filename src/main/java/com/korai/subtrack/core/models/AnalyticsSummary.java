package com.korai.subtrack.core.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummary {

    private int monthlyTotal;
    private int yearlyProjectedTotal;
    private int activeCount;
    private int pausedCount;
    private List<UpcomingItem> upcomingSubscriptions;
    private Map<Category, Integer> categoryBreakdown;
    private List<Subscription> topExpensive;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpcomingItem {
        private Subscription subscription;
        private long daysLeft; // 0: D-Day, 1: D-1
    }
}
