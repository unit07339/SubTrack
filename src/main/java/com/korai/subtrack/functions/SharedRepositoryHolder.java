package com.korai.subtrack.functions;

import com.korai.subtrack.core.repository.InMemorySubscriptionRepository;
import com.korai.subtrack.core.repository.SubscriptionRepository;

public class SharedRepositoryHolder {

    private static final SubscriptionRepository INSTANCE = new InMemorySubscriptionRepository();

    public static SubscriptionRepository getInstance() {
        return INSTANCE;
    }
}
