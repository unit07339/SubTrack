package com.korai.subtrack.core.repository;

import com.korai.subtrack.core.calculator.BillingCalculator;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemorySubscriptionRepository implements SubscriptionRepository {

    private final Map<String, Subscription> store = new ConcurrentHashMap<>();

    @Override
    public Subscription save(Subscription subscription) {
        if (subscription == null) {
            throw new IllegalArgumentException("저장할 구독 정보가 null일 수 없습니다.");
        }
        subscription.validate();

        // 다음 결제일이 미지정된 경우 자동 산출
        if (subscription.getNextBillingDate() == null) {
            LocalDate nextDate = BillingCalculator.calculateNextBillingDate(
                    LocalDate.now(),
                    subscription.getBillingDay(),
                    subscription.getBillingCycle()
            );
            subscription.setNextBillingDate(nextDate);
        }

        subscription.setUpdatedAt(Instant.now());
        store.put(subscription.getId(), subscription);
        return subscription;
    }

    @Override
    public Optional<Subscription> findById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Subscription> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(
                        Subscription::getNextBillingDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByStatus(SubscriptionStatus status) {
        if (status == null) return findAll();
        return store.values().stream()
                .filter(s -> s.getStatus() == status)
                .sorted(Comparator.comparing(
                        Subscription::getNextBillingDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public List<Subscription> findByCategory(Category category) {
        if (category == null) return findAll();
        return store.values().stream()
                .filter(s -> s.getCategory() == category)
                .sorted(Comparator.comparing(
                        Subscription::getNextBillingDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) return false;
        return store.remove(id) != null;
    }

    @Override
    public Subscription updateStatus(String id, SubscriptionStatus status) {
        Subscription subscription = findById(id)
                .orElseThrow(() -> new NoSuchElementException("ID에 해당하는 구독 정보를 찾을 수 없습니다: " + id));

        subscription.setStatus(status);
        subscription.setUpdatedAt(Instant.now());
        store.put(id, subscription);
        return subscription;
    }

    @Override
    public void clear() {
        store.clear();
    }
}
