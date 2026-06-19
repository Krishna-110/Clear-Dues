package com.fairshare.debt_settlement;

import com.fairshare.debt_settlement.dto.CreateDebtRequest;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import com.fairshare.debt_settlement.service.DebtService;
import com.fairshare.debt_settlement.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the "record a repayment -> net the whole relationship" logic in DebtService.
 * Repositories are mocked so no database is needed.
 *
 * Scenario base: A owes B 500 (a PENDING debt with debtor=A, creditor=B).
 * Recording the repayment means "A paid B": who-paid=A (creditor), who-owes=B (debtor),
 * so the new request is debtorPhone=B, creditorPhone=A.
 *
 * Recording collapses every pending debt between the two people into a single net balance,
 * so paying the full amount squares them with no leftover rows.
 */
class DebtServiceSettlementTest {

    private PersonRepository personRepository;
    private DebtRepository debtRepository;
    private SmsService smsService;
    private DebtService debtService;

    private Person a; // phone 1111111111
    private Person b; // phone 2222222222

    @BeforeEach
    void setup() {
        personRepository = mock(PersonRepository.class);
        debtRepository = mock(DebtRepository.class);
        smsService = mock(SmsService.class);
        debtService = new DebtService(personRepository, debtRepository, smsService);

        a = person(1L, "A", "1111111111");
        b = person(2L, "B", "2222222222");

        when(personRepository.findByPhoneNumber("1111111111")).thenReturn(Optional.of(a));
        when(personRepository.findByPhoneNumber("2222222222")).thenReturn(Optional.of(b));
        // save() echoes back its argument
        when(debtRepository.save(any(Debt.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Person person(Long id, String name, String phone) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPhoneNumber(phone);
        p.setEmail(name.toLowerCase() + "@example.com");
        return p;
    }

    private Debt aOwesB(double amount) {
        Debt d = new Debt();
        d.setId(100L);
        d.setDebtor(a);   // A owes
        d.setCreditor(b); // B is owed
        d.setAmount(amount);
        // status defaults to "PENDING"
        return d;
    }

    private Debt between(Long id, Person debtor, Person creditor, double amount) {
        Debt d = new Debt();
        d.setId(id);
        d.setDebtor(debtor);
        d.setCreditor(creditor);
        d.setAmount(amount);
        return d;
    }

    // "A paid B <amount>"  ->  debtor=B, creditor=A
    private CreateDebtRequest repaymentAtoB(double amount) {
        CreateDebtRequest r = new CreateDebtRequest();
        r.setDebtorPhone("2222222222");   // B
        r.setCreditorPhone("1111111111"); // A
        r.setAmount(amount);
        r.setNote("repayment");
        return r;
    }

    @Test
    void exactRepayment_squaresThePairWithNoLeftover() {
        Debt existing = aOwesB(500.0);
        when(debtRepository.findAll()).thenReturn(List.of(existing));

        debtService.createDebt(repaymentAtoB(500.0));

        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        assertThat(existing.getSettledAt()).isNotNull();

        // No new PENDING row should remain - they are square.
        ArgumentCaptor<Debt> captor = ArgumentCaptor.forClass(Debt.class);
        verify(debtRepository, atLeastOnce()).save(captor.capture());
        boolean anyPending = captor.getAllValues().stream().anyMatch(d -> "PENDING".equals(d.getStatus()));
        assertThat(anyPending).isFalse();

        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void partialRepayment_leavesReducedNetStillOwedToCreditor() {
        Debt existing = aOwesB(500.0);
        when(debtRepository.findAll()).thenReturn(List.of(existing));

        debtService.createDebt(repaymentAtoB(300.0));

        // Old row collapsed, replaced by a single net debt: A still owes B 200.
        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        Debt net = capturePending();
        assertThat(net.getDebtor()).isEqualTo(a);
        assertThat(net.getCreditor()).isEqualTo(b);
        assertThat(net.getAmount()).isEqualTo(200.0);
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void overpayment_flipsTheBalanceToTheOtherPerson() {
        Debt existing = aOwesB(500.0);
        when(debtRepository.findAll()).thenReturn(List.of(existing));

        debtService.createDebt(repaymentAtoB(700.0));

        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        // B now owes A the 200 overpayment.
        Debt net = capturePending();
        assertThat(net.getDebtor()).isEqualTo(b);
        assertThat(net.getCreditor()).isEqualTo(a);
        assertThat(net.getAmount()).isEqualTo(200.0);
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void settleFullyBalancedGroups_crossesOutAClosedCycle() {
        // Skylar -> Suket 500, Suket -> Morpheus 500, Morpheus -> Skylar 500 (the settle payment).
        // The whole group nets to zero, so all three debts should be marked SETTLED.
        Person skylar = person(10L, "Skylar", "9999999999");
        Person suket = person(11L, "Suket", "8888888888");
        Person morpheus = person(12L, "Morpheus", "7777777777");
        Debt d1 = between(201L, skylar, suket, 500.0);
        Debt d2 = between(202L, suket, morpheus, 500.0);
        Debt d3 = between(203L, morpheus, skylar, 500.0);
        when(debtRepository.findAll()).thenReturn(List.of(d1, d2, d3));

        java.util.Set<Long> settled = debtService.settleFullyBalancedGroups();

        assertThat(settled).containsExactlyInAnyOrder(201L, 202L, 203L);
        assertThat(d1.getStatus()).isEqualTo("SETTLED");
        assertThat(d2.getStatus()).isEqualTo("SETTLED");
        assertThat(d3.getStatus()).isEqualTo("SETTLED");
        assertThat(d1.getSettledAt()).isNotNull();
    }

    @Test
    void settleFullyBalancedGroups_leavesAnUnbalancedGroupUntouched() {
        // Only the two chain debts exist, no closing payment yet -> nobody is squared.
        Person skylar = person(10L, "Skylar", "9999999999");
        Person suket = person(11L, "Suket", "8888888888");
        Person morpheus = person(12L, "Morpheus", "7777777777");
        Debt d1 = between(201L, skylar, suket, 500.0);
        Debt d2 = between(202L, suket, morpheus, 500.0);
        when(debtRepository.findAll()).thenReturn(List.of(d1, d2));

        java.util.Set<Long> settled = debtService.settleFullyBalancedGroups();

        assertThat(settled).isEmpty();
        assertThat(d1.getStatus()).isEqualTo("PENDING");
        assertThat(d2.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void noMatchingDebt_createsNormalNewPendingDebtAndSendsSms() {
        when(debtRepository.findAll()).thenReturn(Collections.emptyList());

        Debt result = debtService.createDebt(repaymentAtoB(500.0));

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getDebtor()).isEqualTo(b);
        assertThat(result.getCreditor()).isEqualTo(a);
        assertThat(result.getAmount()).isEqualTo(500.0);
        // a brand-new debt notifies the debtor
        verify(smsService, times(1)).sendDebtNotification(eq("2222222222"), eq("A"), anyString(), anyBoolean(), eq("B"));
    }

    // Returns the (last) PENDING debt saved during the call.
    private Debt capturePending() {
        ArgumentCaptor<Debt> captor = ArgumentCaptor.forClass(Debt.class);
        verify(debtRepository, atLeast(1)).save(captor.capture());
        return captor.getAllValues().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .reduce((first, second) -> second)
                .orElseThrow();
    }
}
