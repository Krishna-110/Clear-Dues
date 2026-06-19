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
 * Verifies that settlement suggestions are PAIRWISE (a direct net between two people)
 * and never routed through a third person. The amount shown for "A pays B" must equal
 * exactly what A owes B, so that recording that repayment squares them.
 */
class SettlementServicePairwiseTest {

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
    void mutualDebts_netToASingleSuggestion() {
        Person a = person(1L, "A", "1111111111");
        Person b = person(2L, "B", "2222222222");
        // A owes B 500, B owes A 200 -> net: A owes B 300
        when(debtRepository.findAll()).thenReturn(List.of(
                debt(1L, a, b, 500.0, "PENDING"),
                debt(2L, b, a, 200.0, "PENDING")
        ));

        List<SettlementResponse> result = settlementService.settleDebts();

        assertThat(result).hasSize(1);
        SettlementResponse s = result.get(0);
        assertThat(s.getFromPhone()).isEqualTo("1111111111"); // A pays
        assertThat(s.getToPhone()).isEqualTo("2222222222");   // B receives
        assertThat(s.getAmount()).isEqualTo(300.0);
    }

    @Test
    void chainOfDebts_staysPairwise_noRoutingThroughThirdPerson() {
        Person a = person(1L, "A", "1111111111");
        Person b = person(2L, "B", "2222222222");
        Person c = person(3L, "C", "3333333333");
        // A owes B 500, B owes C 500.
        // A global optimizer would collapse this to "A pays C 500" (routing through B).
        // Pairwise keeps both real debts so each number is settleable directly.
        when(debtRepository.findAll()).thenReturn(List.of(
                debt(1L, a, b, 500.0, "PENDING"),
                debt(2L, b, c, 500.0, "PENDING")
        ));

        List<SettlementResponse> result = settlementService.settleDebts();

        assertThat(result).hasSize(2);
        boolean routedAtoC = result.stream().anyMatch(s ->
                s.getFromPhone().equals("1111111111") && s.getToPhone().equals("3333333333"));
        assertThat(routedAtoC).isFalse();
        assertThat(result).anyMatch(s ->
                s.getFromPhone().equals("1111111111") && s.getToPhone().equals("2222222222") && s.getAmount() == 500.0);
        assertThat(result).anyMatch(s ->
                s.getFromPhone().equals("2222222222") && s.getToPhone().equals("3333333333") && s.getAmount() == 500.0);
    }

    @Test
    void fullySquaredPair_producesNoSuggestion() {
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
