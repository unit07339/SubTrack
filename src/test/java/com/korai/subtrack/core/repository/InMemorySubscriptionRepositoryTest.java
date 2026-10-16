package com.korai.subtrack.core.repository;

import com.korai.subtrack.core.models.BillingCycle;
import com.korai.subtrack.core.models.Category;
import com.korai.subtrack.core.models.Subscription;
import com.korai.subtrack.core.models.SubscriptionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InMemorySubscriptionRepository 단위 테스트")
public class InMemorySubscriptionRepositoryTest {

    private InMemorySubscriptionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemorySubscriptionRepository();
        repository.clear();
    }

    @Test
    @DisplayName("구독 신규 등록 및 ID로 단건 조회 검증")
    void testSaveAndFindById() {
        Subscription sub = Subscription.builder()
                .name("Netflix")
                .cost(17000)
                .billingDay(25)
                .category(Category.ENTERTAINMENT)
                .build();

        Subscription saved = repository.save(sub);

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getNextBillingDate()).isNotNull();

        Optional<Subscription> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Netflix");
        assertThat(found.get().getCost()).isEqualTo(17000);
    }

    @Test
    @DisplayName("상태별(ACTIVE/PAUSED) 및 카테고리별 필터링 조회 검증")
    void testFilterByStatusAndCategory() {
        Subscription sub1 = Subscription.builder()
                .name("Netflix")
                .cost(17000)
                .billingDay(10)
                .category(Category.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription sub2 = Subscription.builder()
                .name("ChatGPT")
                .cost(29000)
                .billingDay(15)
                .category(Category.PRODUCTIVITY)
                .status(SubscriptionStatus.PAUSED)
                .build();

        Subscription sub3 = Subscription.builder()
                .name("YouTube")
                .cost(14900)
                .billingDay(20)
                .category(Category.ENTERTAINMENT)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        repository.save(sub1);
        repository.save(sub2);
        repository.save(sub3);

        List<Subscription> all = repository.findAll();
        assertThat(all).hasSize(3);

        List<Subscription> activeList = repository.findByStatus(SubscriptionStatus.ACTIVE);
        assertThat(activeList).hasSize(2).extracting("name").containsExactlyInAnyOrder("Netflix", "YouTube");

        List<Subscription> pausedList = repository.findByStatus(SubscriptionStatus.PAUSED);
        assertThat(pausedList).hasSize(1).extracting("name").containsExactly("ChatGPT");

        List<Subscription> entertainmentList = repository.findByCategory(Category.ENTERTAINMENT);
        assertThat(entertainmentList).hasSize(2);
    }

    @Test
    @DisplayName("구독 상태 변경(updateStatus) 및 미존재 ID 예외 검증")
    void testUpdateStatus() {
        Subscription sub = Subscription.builder()
                .name("Spotify")
                .cost(10900)
                .billingDay(5)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        Subscription saved = repository.save(sub);

        Subscription updated = repository.updateStatus(saved.getId(), SubscriptionStatus.PAUSED);
        assertThat(updated.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);

        assertThatThrownBy(() -> repository.updateStatus("non-existent-id", SubscriptionStatus.CANCELLED))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("구독 삭제(deleteById) 및 clear 검증")
    void testDeleteAndClear() {
        Subscription sub = Subscription.builder()
                .name("Coupang")
                .cost(7890)
                .billingDay(1)
                .build();

        Subscription saved = repository.save(sub);
        assertThat(repository.deleteById(saved.getId())).isTrue();
        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.deleteById("non-existent")).isFalse();

        repository.save(sub);
        assertThat(repository.findAll()).isNotEmpty();
        repository.clear();
        assertThat(repository.findAll()).isEmpty();
    }
}
