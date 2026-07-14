package com.fairshare.debt_settlement;

import com.fairshare.debt_settlement.dto.GamificationResponse;
import com.fairshare.debt_settlement.dto.GamificationResponse.Badge;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import com.fairshare.debt_settlement.service.GamificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GamificationServiceTest {

    private DebtRepository debtRepository;
    private PersonRepository personRepository;
    private GamificationService gamificationService;

    @BeforeEach
    void setup() {
        debtRepository = mock(DebtRepository.class);
        personRepository = mock(PersonRepository.class);
        gamificationService = new GamificationService(debtRepository, personRepository);

        Person user = new Person();
        user.setId(1L);
        user.setEmail("a@x.com");
        when(personRepository.findByEmail("a@x.com")).thenReturn(Optional.of(user));
    }

    private Debt debt(String status, LocalDateTime settledAt) {
        Debt d = new Debt();
        d.setStatus(status);
        d.setSettledAt(settledAt);
        return d;
    }

    private Badge badge(GamificationResponse r, String key) {
        return r.getBadges().stream().filter(b -> b.getKey().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void progressAndBadges_reflectSettledVsPending() {
        // 2 settled (this week) + 1 pending -> 67% progress, streak >= 1
        when(debtRepository.findAllTransactionsForUser(1L)).thenReturn(List.of(
                debt("SETTLED", LocalDateTime.now()),
                debt("SETTLED", LocalDateTime.now()),
                debt("PENDING", null)
        ));

        GamificationResponse r = gamificationService.forUser("a@x.com");

        assertThat(r.getSettledCount()).isEqualTo(2);
        assertThat(r.getPendingCount()).isEqualTo(1);
        assertThat(r.getProgressPercent()).isEqualTo(67);
        assertThat(r.getStreakWeeks()).isGreaterThanOrEqualTo(1);
        assertThat(badge(r, "FIRST_SETTLEMENT").isUnlocked()).isTrue();
        assertThat(badge(r, "SETTLED_CIRCLE").isUnlocked()).isFalse(); // still has a pending
    }

    @Test
    void settledCircle_unlocksWhenNothingPending() {
        when(debtRepository.findAllTransactionsForUser(1L)).thenReturn(List.of(
                debt("SETTLED", LocalDateTime.now())
        ));

        GamificationResponse r = gamificationService.forUser("a@x.com");

        assertThat(r.getProgressPercent()).isEqualTo(100);
        assertThat(badge(r, "SETTLED_CIRCLE").isUnlocked()).isTrue();
    }

    @Test
    void noDebts_isZeroProgressNoStreak() {
        when(debtRepository.findAllTransactionsForUser(1L)).thenReturn(List.of());

        GamificationResponse r = gamificationService.forUser("a@x.com");

        assertThat(r.getProgressPercent()).isZero();
        assertThat(r.getStreakWeeks()).isZero();
        assertThat(badge(r, "FIRST_SETTLEMENT").isUnlocked()).isFalse();
    }
}
