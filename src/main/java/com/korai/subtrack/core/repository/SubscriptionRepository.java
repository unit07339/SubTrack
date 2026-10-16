package com.korai.subtrack.core.repository;

import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {
    Subscription save(Subscription subscription);

    Optional<Subscription> findById(String id);

    List<Subscription> findAll();

    List<Subscription> findByStatus(SubscriptionStatus status);

    List<Subscription> findByCategory(Category category);

    boolean deleteById(String id);

    Subscription updateStatus(String id, SubscriptionStatus status);

    void clear();
}
