package com.korai.subtrack.core.models;

public enum Category {
    ENTERTAINMENT("엔터테인먼트"),
    PRODUCTIVITY("생산성/업무"),
    MUSIC("음원/오디오"),
    LIFESTYLE("생활/쇼핑"),
    EDUCATION("교육/도서"),
    OTHER("기타");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
