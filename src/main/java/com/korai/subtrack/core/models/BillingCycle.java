package com.korai.subtrack.core.models;

public enum BillingCycle {
    MONTHLY("월간"),
    YEARLY("연간"),
    WEEKLY("주간");

    private final String displayName;

    BillingCycle(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
