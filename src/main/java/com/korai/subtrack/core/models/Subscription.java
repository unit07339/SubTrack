package com.korai.subtrack.core.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {
    private String id;
    private String name;
    private int cost;
    private String currency; // "KRW", "USD"
    private BillingCycle billingCycle;
    private int billingDay; // 1 ~ 31

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextBillingDate;

    private Category category;
    private String paymentMethod;
    private SubscriptionStatus status;
    private String iconKey;
    private String memo;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    public void validate() {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("서비스명(name)은 필수 입력 항목입니다.");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("결제 금액(cost)은 0원 이상이어야 합니다.");
        }
        if (billingDay < 1 || billingDay > 31) {
            throw new IllegalArgumentException("결제일(billingDay)은 1일부터 31일 사이여야 합니다.");
        }
        if (billingCycle == null) {
            this.billingCycle = BillingCycle.MONTHLY;
        }
        if (category == null) {
            this.category = Category.OTHER;
        }
        if (status == null) {
            this.status = SubscriptionStatus.ACTIVE;
        }
        if (currency == null || currency.trim().isEmpty()) {
            this.currency = "KRW";
        }
        if (id == null || id.trim().isEmpty()) {
            this.id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (updatedAt == null) {
            this.updatedAt = this.createdAt;
        }
    }
}
