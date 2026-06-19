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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for recording debts and the automatic ledger simplification that follows.
 *
 * A stateful in-memory repo stands in for the database: save() assigns ids and stores rows,
 * findAll() returns them - so createDebt -> simplifyConnectedComponents behaves end to end.
 *
 * "X paid Y <amount>" is recorded as who-paid=X (creditor), who-owes=Y (debtor).
 */
class DebtServiceSettlementTest {

    private PersonRepository personRepository;
    private DebtRepository debtRepository;
    private SmsService smsService;
    private DebtService debtService;

    private List<Debt> db;
    private long nextId;

    private Person a; // 1111111111
    private Person b; // 2222222222
    private Person c; // 3333333333

    @BeforeEach
    void setup() {
        personRepository = mock(PersonRepository.class);
        debtRepository = mock(DebtRepository.class);
        smsService = mock(SmsService.class);
        debtService = new DebtService(personRepository, debtRepository, smsService);

        a = person(1L, "A", "1111111111");
        b = person(2L, "B", "2222222222");
        c = person(3L, "C", "3333333333");

        when(personRepository.findByPhoneNumber("1111111111")).thenReturn(Optional.of(a));
        when(personRepository.findByPhoneNumber("2222222222")).thenReturn(Optional.of(b));
        when(personRepository.findByPhoneNumber("3333333333")).thenReturn(Optional.of(c));

        // Stateful fake repository.
        db = new ArrayList<>();
        nextId = 1000L;
        when(debtRepository.save(any(Debt.class))).thenAnswer(inv -> {
            Debt d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId(nextId++);
                db.add(d);
            }
            return d;
        });
        when(debtRepository.findAll()).thenAnswer(inv -> new ArrayList<>(db));
    }

    private Person person(Long id, String name, String phone) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setPhoneNumber(phone);
        p.setEmail(name.toLowerCase() + "@example.com");
        return p;
    }

    private Debt seed(Long id, Person debtor, Person creditor, double amount) {
        Debt d = new Debt();
        d.setId(id);
        d.setDebtor(debtor);
        d.setCreditor(creditor);
        d.setAmount(amount);
        db.add(d);
        return d;
    }

    private CreateDebtRequest req(String debtorPhone, String creditorPhone, double amount) {
        CreateDebtRequest r = new CreateDebtRequest();
        r.setDebtorPhone(debtorPhone);
        r.setCreditorPhone(creditorPhone);
        r.setAmount(amount);
        r.setNote("test");
        return r;
    }

    private List<Debt> pending() {
        return db.stream().filter(d -> "PENDING".equals(d.getStatus())).collect(Collectors.toList());
    }

    @Test
    void newStandaloneDebt_staysPendingAndNotifiesDebtor() {
        Debt result = debtService.createDebt(req("2222222222", "1111111111", 500.0)); // B owes A

        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(pending()).hasSize(1);
        assertThat(result.getDebtor()).isEqualTo(b);
        assertThat(result.getCreditor()).isEqualTo(a);
        verify(smsService, times(1)).sendDebtNotification(eq("2222222222"), eq("A"), anyString(), anyBoolean(), eq("B"));
    }

    @Test
    void exactRepayment_squaresThePairWithNoLeftover() {
        Debt existing = seed(100L, a, b, 500.0); // A owes B 500
        debtService.createDebt(req("2222222222", "1111111111", 500.0)); // "A paid B" -> B owes A 500

        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        assertThat(pending()).isEmpty();
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void partialRepayment_leavesReducedNetOwedToCreditor() {
        Debt existing = seed(100L, a, b, 500.0); // A owes B 500
        debtService.createDebt(req("2222222222", "1111111111", 300.0)); // pays 300

        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        List<Debt> remaining = pending();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getDebtor()).isEqualTo(a); // A still owes B
        assertThat(remaining.get(0).getCreditor()).isEqualTo(b);
        assertThat(remaining.get(0).getAmount()).isEqualTo(200.0);
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void overpayment_flipsTheBalanceToTheOtherPerson() {
        Debt existing = seed(100L, a, b, 500.0); // A owes B 500
        debtService.createDebt(req("2222222222", "1111111111", 700.0)); // pays 700

        assertThat(existing.getStatus()).isEqualTo("SETTLED");
        List<Debt> remaining = pending();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getDebtor()).isEqualTo(b); // B now owes A
        assertThat(remaining.get(0).getCreditor()).isEqualTo(a);
        assertThat(remaining.get(0).getAmount()).isEqualTo(200.0);
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }

    @Test
    void chainCollapses_middlemanIsSettledAndDirectDebtCreated() {
        // B owes A 500 already; now C owes B 500 -> chain C -> B -> A.
        Debt bOwesA = seed(100L, b, a, 500.0);
        debtService.createDebt(req("3333333333", "2222222222", 500.0)); // C owes B 500

        // The middleman's debts are settled...
        assertThat(bOwesA.getStatus()).isEqualTo("SETTLED");
        assertThat(db.stream().anyMatch(d ->
                d.getDebtor() == c && d.getCreditor() == b && "SETTLED".equals(d.getStatus()))).isTrue();

        // ...and a single direct debt C owes A 500 remains.
        List<Debt> remaining = pending();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getDebtor()).isEqualTo(c);
        assertThat(remaining.get(0).getCreditor()).isEqualTo(a);
        assertThat(remaining.get(0).getAmount()).isEqualTo(500.0);
    }

    @Test
    void payingTheSimplifiedDebt_settlesEverything() {
        // After a chain collapsed, C owes A 500 directly. C now pays A.
        seed(100L, c, a, 500.0); // C owes A 500
        debtService.createDebt(req("1111111111", "3333333333", 500.0)); // "C paid A" -> A owes C 500

        assertThat(pending()).isEmpty();
        assertThat(db).allMatch(d -> "SETTLED".equals(d.getStatus()));
        verify(smsService, never()).sendDebtNotification(anyString(), anyString(), anyString(), anyBoolean(), anyString());
    }
}
