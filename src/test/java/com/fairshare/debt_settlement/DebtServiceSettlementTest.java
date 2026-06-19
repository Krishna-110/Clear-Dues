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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the "record a repayment -> settle the existing debt" logic in DebtService.
 * Repositories are mocked so no database is needed.
 *
 * Scenario base: A owes B 500 (a PENDING debt with debtor=A, creditor=B).
 * Recording the repayment means "A paid B": who-paid=A (creditor), who-owes=B (debtor),
 * so the new request is debtorPhone=B, creditorPhone=A.
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
    void exactRepayment_marksExistingDebtSettled() {
        Debt existing = aOwesB(500.0);
        when(debtRepository.findAll()).thenReturn(List.of(existing));

        debtService.createDebt(repaymentAtoB(500.0));

        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        assertThat(existing.getSettledAt()).isNotNull();
        // no new debt stacked, no SMS for a settlement
        verify(debtRepository, times(1)).save(existing);
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void partialRepayment_shrinksExistingDebtAndKeepsItPending() {
        Debt existing = aOwesB(500.0);
        when(debtRepository.findAll()).thenReturn(List.of(existing));

        debtService.createDebt(repaymentAtoB(300.0));

        assertThat(existing.getStatus()).isEqualTo("PENDING");
        assertThat(existing.getAmount()).isEqualTo(200.0);
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void overpayment_settlesExistingAndCreatesReverseDebtForRemainder() {
        Debt existing = aOwesB(500.0);
        when(debtRepository.findAll()).thenReturn(List.of(existing));

        debtService.createDebt(repaymentAtoB(700.0));

        assertThat(existing.getStatus()).isEqualTo("SETTLED");

        ArgumentCaptor<Debt> captor = ArgumentCaptor.forClass(Debt.class);
        verify(debtRepository, atLeast(2)).save(captor.capture());
        // the reverse leftover debt: B owes A 200, PENDING
        Debt leftover = captor.getAllValues().stream()
                .filter(d -> "PENDING".equals(d.getStatus()) && d.getDebtor() == b && d.getCreditor() == a)
                .findFirst().orElseThrow();
        assertThat(leftover.getAmount()).isEqualTo(200.0);
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
}
