package com.korai.subtrack.core.models;

public enum SubscriptionStatus {
    ACTIVE("활성"),
    PAUSED("일시정지"),
    CANCELLED("해지완료");

    private final String displayName;

    SubscriptionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
