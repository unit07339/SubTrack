package com.korai.subtrack.core.calculator;

import com.korai.subtrack.core.models.BillingCycle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BillingCalculator 결제 계산기 단위 테스트")
public class BillingCalculatorTest {

    @Test
    @DisplayName("당월 결제일이 아직 지나지 않은 경우 당월 결제일 반환")
    void testNextBillingDate_CurrentMonth() {
        LocalDate ref = LocalDate.of(2026, 10, 10);
        LocalDate next = BillingCalculator.calculateNextBillingDate(ref, 25, BillingCycle.MONTHLY);

        assertThat(next).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(BillingCalculator.calculateDaysLeft(ref, next)).isEqualTo(15);
    }

    @Test
    @DisplayName("당월 결제일 당일인 경우 오늘 일자 반환 및 D-Day(0) 확인")
    void testNextBillingDate_Today() {
        LocalDate ref = LocalDate.of(2026, 10, 25);
        LocalDate next = BillingCalculator.calculateNextBillingDate(ref, 25, BillingCycle.MONTHLY);

        assertThat(next).isEqualTo(LocalDate.of(2026, 10, 25));
        assertThat(BillingCalculator.calculateDaysLeft(ref, next)).isEqualTo(0);
    }

    @Test
    @DisplayName("당월 결제일이 이미 지난 경우 익월 결제일 반환")
    void testNextBillingDate_NextMonth() {
        LocalDate ref = LocalDate.of(2026, 10, 26);
        LocalDate next = BillingCalculator.calculateNextBillingDate(ref, 25, BillingCycle.MONTHLY);

        assertThat(next).isEqualTo(LocalDate.of(2026, 11, 25));
        assertThat(BillingCalculator.calculateDaysLeft(ref, next)).isEqualTo(30);
    }

    @Test
    @DisplayName("말일(31일) 결제 설정 시 30일/28일 월에 대한 안전한 클램핑 검증")
    void testNextBillingDate_MonthEndClamping() {
        // 10월 31일 이후 다음 결제월은 11월(30일까지 존재) -> 11월 30일
        LocalDate refOct = LocalDate.of(2026, 10, 31);
        LocalDate nextNov = BillingCalculator.calculateNextBillingDate(refOct.plusDays(1), 31, BillingCycle.MONTHLY);
        assertThat(nextNov).isEqualTo(LocalDate.of(2026, 11, 30));

        // 1월 31일 이후 2월(평년 28일) -> 2월 28일
        LocalDate refJan = LocalDate.of(2027, 2, 1);
        LocalDate nextFeb = BillingCalculator.calculateNextBillingDate(refJan, 31, BillingCycle.MONTHLY);
        assertThat(nextFeb).isEqualTo(LocalDate.of(2027, 2, 28));

        // 윤년 2028년 2월(29일) -> 2월 29일
        LocalDate refLeap = LocalDate.of(2028, 2, 1);
        LocalDate nextLeap = BillingCalculator.calculateNextBillingDate(refLeap, 31, BillingCycle.MONTHLY);
        assertThat(nextLeap).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    @DisplayName("잘못된 결제일 입력 시 예외 발생 검증")
    void testInvalidBillingDay() {
        LocalDate ref = LocalDate.of(2026, 10, 16);
        assertThatThrownBy(() -> BillingCalculator.calculateNextBillingDate(ref, 0, BillingCycle.MONTHLY))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> BillingCalculator.calculateNextBillingDate(ref, 32, BillingCycle.MONTHLY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("결제 주기별 월 환산액 및 연간 환산액 산출 검증")
    void testCostConversions() {
        // 월 17,000원
        assertThat(BillingCalculator.toMonthlyEquivalent(17000, BillingCycle.MONTHLY)).isEqualTo(17000);
        assertThat(BillingCalculator.toYearlyProjected(17000, BillingCycle.MONTHLY)).isEqualTo(204000);

        // 연 120,000원 -> 월 10,000원
        assertThat(BillingCalculator.toMonthlyEquivalent(120000, BillingCycle.YEARLY)).isEqualTo(10000);
        assertThat(BillingCalculator.toYearlyProjected(120000, BillingCycle.YEARLY)).isEqualTo(120000);

        // 주 3,000원 -> 연 156,000원 -> 월 13,000원
        assertThat(BillingCalculator.toYearlyProjected(3000, BillingCycle.WEEKLY)).isEqualTo(156000);
        assertThat(BillingCalculator.toMonthlyEquivalent(3000, BillingCycle.WEEKLY)).isEqualTo(13000);
    }
}
