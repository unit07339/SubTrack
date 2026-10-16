package com.korai.subtrack.core.calculator;

import com.korai.subtrack.core.models.BillingCycle;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * 구독 서비스의 결제일, D-Day, 결제 주기별 금액 환산을 처리하는 계산기
 */
public class BillingCalculator {

    /**
     * 기준일(referenceDate)을 기준으로 다음 결제 예정일을 계산합니다.
     * 말일(예: 31일)인 경우 해당 월의 마지막 일수로 안전하게 클램핑(Clamping)합니다.
     */
    public static LocalDate calculateNextBillingDate(LocalDate referenceDate, int billingDay, BillingCycle cycle) {
        if (billingDay < 1 || billingDay > 31) {
            throw new IllegalArgumentException("결제일(billingDay)은 1부터 31 사이여야 합니다.");
        }
        if (cycle == null) {
            cycle = BillingCycle.MONTHLY;
        }

        switch (cycle) {
            case MONTHLY:
                return calculateNextMonthlyBillingDate(referenceDate, billingDay);
            case YEARLY:
                return calculateNextYearlyBillingDate(referenceDate, billingDay);
            case WEEKLY:
                return referenceDate.plusWeeks(1);
            default:
                return calculateNextMonthlyBillingDate(referenceDate, billingDay);
        }
    }

    private static LocalDate calculateNextMonthlyBillingDate(LocalDate referenceDate, int billingDay) {
        YearMonth currentYm = YearMonth.from(referenceDate);
        int clampedDayThisMonth = Math.min(billingDay, currentYm.lengthOfMonth());
        LocalDate candidateThisMonth = currentYm.atDay(clampedDayThisMonth);

        if (!candidateThisMonth.isBefore(referenceDate)) {
            return candidateThisMonth;
        }

        YearMonth nextYm = currentYm.plusMonths(1);
        int clampedDayNextMonth = Math.min(billingDay, nextYm.lengthOfMonth());
        return nextYm.atDay(clampedDayNextMonth);
    }

    private static LocalDate calculateNextYearlyBillingDate(LocalDate referenceDate, int billingDay) {
        YearMonth thisMonth = YearMonth.from(referenceDate);
        int clampedDay = Math.min(billingDay, thisMonth.lengthOfMonth());
        LocalDate candidate = thisMonth.atDay(clampedDay);

        if (!candidate.isBefore(referenceDate)) {
            return candidate;
        }
        YearMonth nextYearSameMonth = thisMonth.plusYears(1);
        int clampedDayNextYear = Math.min(billingDay, nextYearSameMonth.lengthOfMonth());
        return nextYearSameMonth.atDay(clampedDayNextYear);
    }

    /**
     * 기준일로부터 다음 결제일까지 남은 일수(D-Day)를 계산합니다.
     * 0이면 오늘 결제(D-Day), 양수이면 남은 일수(D-N)입니다.
     */
    public static long calculateDaysLeft(LocalDate referenceDate, LocalDate nextBillingDate) {
        return ChronoUnit.DAYS.between(referenceDate, nextBillingDate);
    }

    /**
     * 결제 주기에 따른 월 환산 금액을 산출합니다.
     */
    public static int toMonthlyEquivalent(int cost, BillingCycle cycle) {
        if (cycle == null) return cost;
        return switch (cycle) {
            case MONTHLY -> cost;
            case YEARLY -> (int) Math.round((double) cost / 12.0);
            case WEEKLY -> (int) Math.round(((double) cost * 52.0) / 12.0);
        };
    }

    /**
     * 결제 주기에 따른 연간 환산 누적 금액을 산출합니다.
     */
    public static int toYearlyProjected(int cost, BillingCycle cycle) {
        if (cycle == null) return cost * 12;
        return switch (cycle) {
            case MONTHLY -> cost * 12;
            case YEARLY -> cost;
            case WEEKLY -> cost * 52;
        };
    }
}
