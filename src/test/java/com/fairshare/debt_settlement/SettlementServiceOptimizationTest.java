package com.fairshare.debt_settlement;

import com.fairshare.debt_settlement.dto.SettlementResponse;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Verifies the OPTIMIZED (global min-cash-flow) settlement suggestions: chains are
 * collapsed so pass-through people drop out and the result is the fewest transfers.
 */
class SettlementServiceOptimizationTest {

    private DebtRepository debtRepository;
    private SettlementService settlementService;

    @BeforeEach
    void setup() {
        debtRepository = mock(DebtRepository.class);
        settlementService = new SettlementService(debtRepository);
    }

    private Person person(Long id, String name, String phone) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPhoneNumber(phone);
        return p;
    }

    private Debt debt(Long id, Person debtor, Person creditor, double amount, String status) {
        Debt d = new Debt();
        d.setId(id);
        d.setDebtor(debtor);
        d.setCreditor(creditor);
        d.setAmount(amount);
        d.setStatus(status);
        return d;
    }

    @Test
    void chainCollapses_passThroughPersonDropsOut() {
        Person skylar = person(1L, "Skylar", "1111111111");
        Person suket = person(2L, "Suket", "2222222222");
        Person morpheus = person(3L, "Morpheus", "3333333333");
        // Skylar owes Suket 500, Suket owes Morpheus 500.
        // Optimized: Suket is a pass-through -> Skylar pays Morpheus 500 directly.
        when(debtRepository.findAll()).thenReturn(List.of(
                debt(1L, skylar, suket, 500.0, "PENDING"),
                debt(2L, suket, morpheus, 500.0, "PENDING")
        ));

        List<SettlementResponse> result = settlementService.settleDebts();

        assertThat(result).hasSize(1);
        SettlementResponse s = result.get(0);
        assertThat(s.getFromPhone()).isEqualTo("1111111111"); // Skylar pays
        assertThat(s.getToPhone()).isEqualTo("3333333333");   // Morpheus receives
        assertThat(s.getAmount()).isEqualTo(500.0);
        // Suket should not appear at all - he nets to zero.
        assertThat(result).noneMatch(r ->
                r.getFromPhone().equals("2222222222") || r.getToPhone().equals("2222222222"));
    }

    @Test
    void mutualDebts_netToASingleSuggestion() {
        Person a = person(1L, "A", "1111111111");
        Person b = person(2L, "B", "2222222222");
        // A owes B 500, B owes A 200 -> net A owes B 300
        when(debtRepository.findAll()).thenReturn(List.of(
                debt(1L, a, b, 500.0, "PENDING"),
                debt(2L, b, a, 200.0, "PENDING")
        ));

        List<SettlementResponse> result = settlementService.settleDebts();

        assertThat(result).hasSize(1);
        SettlementResponse s = result.get(0);
        assertThat(s.getFromPhone()).isEqualTo("1111111111");
        assertThat(s.getToPhone()).isEqualTo("2222222222");
        assertThat(s.getAmount()).isEqualTo(300.0);
    }

    @Test
    void everyoneSquared_producesNoSuggestions() {
        Person a = person(1L, "A", "1111111111");
        Person b = person(2L, "B", "2222222222");
        // equal and opposite -> squared
        when(debtRepository.findAll()).thenReturn(List.of(
                debt(1L, a, b, 400.0, "PENDING"),
                debt(2L, b, a, 400.0, "PENDING")
        ));

        assertThat(settlementService.settleDebts()).isEmpty();
    }

    @Test
    void settledDebtsAreIgnored() {
        Person a = person(1L, "A", "1111111111");
        Person b = person(2L, "B", "2222222222");
        when(debtRepository.findAll()).thenReturn(List.of(
                debt(1L, a, b, 500.0, "SETTLED")
        ));

        assertThat(settlementService.settleDebts()).isEmpty();
    }
}
