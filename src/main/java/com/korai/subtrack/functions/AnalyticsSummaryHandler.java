package com.korai.subtrack.functions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.korai.subtrack.core.calculator.BillingCalculator;
import com.korai.subtrack.core.models.AnalyticsSummary;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;
import com.korai.subtrack.core.repository.SubscriptionRepository;
import com.korai.subtrack.functions.common.ApiResponse;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AnalyticsSummaryHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final SubscriptionRepository repository;
    private final Clock clock;

    public AnalyticsSummaryHandler() {
        this(SharedRepositoryHolder.getInstance(), Clock.systemDefaultZone());
    }

    public AnalyticsSummaryHandler(SubscriptionRepository repository) {
        this(repository, Clock.systemDefaultZone());
    }

    public AnalyticsSummaryHandler(SubscriptionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LocalDate today = LocalDate.now(clock);
        List<Subscription> all = repository.findAll();

        int monthlyTotal = 0;
        int yearlyProjectedTotal = 0;
        int activeCount = 0;
        int pausedCount = 0;
        Map<Category, Integer> categoryBreakdown = new EnumMap<>(Category.class);

        // 카테고리 맵 0원 초기화
        for (Category cat : Category.values()) {
            categoryBreakdown.put(cat, 0);
        }

        List<AnalyticsSummary.UpcomingItem> upcomingList = new ArrayList<>();

        for (Subscription sub : all) {
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                activeCount++;
                int monthlyCost = BillingCalculator.toMonthlyEquivalent(sub.getCost(), sub.getBillingCycle());
                int yearlyCost = BillingCalculator.toYearlyProjected(sub.getCost(), sub.getBillingCycle());

                monthlyTotal += monthlyCost;
                yearlyProjectedTotal += yearlyCost;

                categoryBreakdown.put(
                        sub.getCategory(),
                        categoryBreakdown.getOrDefault(sub.getCategory(), 0) + monthlyCost
                );

                if (sub.getNextBillingDate() != null) {
                    long daysLeft = BillingCalculator.calculateDaysLeft(today, sub.getNextBillingDate());
                    if (daysLeft >= 0 && daysLeft <= 7) {
                        upcomingList.add(new AnalyticsSummary.UpcomingItem(sub, daysLeft));
                    }
                }
            } else if (sub.getStatus() == SubscriptionStatus.PAUSED) {
                pausedCount++;
            }
        }

        // 다가오는 결제일 오름차순 정렬 (0일 남은 D-Day 우선)
        upcomingList.sort(Comparator.comparingLong(AnalyticsSummary.UpcomingItem::getDaysLeft));

        // 가장 비싼 구독 Top 5 (월 환산액 기준 내림차순)
        List<Subscription> topExpensive = all.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .sorted(Comparator.comparingInt(
                        (Subscription s) -> BillingCalculator.toMonthlyEquivalent(s.getCost(), s.getBillingCycle())
                ).reversed())
                .limit(5)
                .collect(Collectors.toList());

        AnalyticsSummary summary = AnalyticsSummary.builder()
                .monthlyTotal(monthlyTotal)
                .yearlyProjectedTotal(yearlyProjectedTotal)
                .activeCount(activeCount)
                .pausedCount(pausedCount)
                .upcomingSubscriptions(upcomingList)
                .categoryBreakdown(categoryBreakdown)
                .topExpensive(topExpensive)
                .build();

        return ApiResponse.success(200, summary);
    }
}
