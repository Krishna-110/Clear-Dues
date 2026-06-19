package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.SettlementResponse;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.repository.DebtRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SettlementService {

    private final DebtRepository debtRepository;

    public SettlementService(DebtRepository debtRepository) {
        this.debtRepository = debtRepository;
    }

    /**
     * Returns the OPTIMIZED set of payments that squares everyone with the fewest
     * transfers (greedy min-cash-flow). This collapses chains: if Skylar owes Suket
     * and Suket owes Morpheus the same amount, Suket drops out and the suggestion
     * becomes "Skylar pays Morpheus" directly.
     *
     * This is a read-only suggestion. Recording the corresponding payment in the app
     * (which adds the matching debt) makes everyone's net balance zero, so the
     * suggestion disappears - i.e. it settles as expected.
     */
    public List<SettlementResponse> settleDebts() {
        Set<Long> seen = new HashSet<>();
        List<Debt> pending = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> d.getDebtor() != null && d.getCreditor() != null)
                .filter(d -> d.getDebtor().getPhoneNumber() != null && d.getCreditor().getPhoneNumber() != null)
                .filter(d -> seen.add(d.getId())) // guard against any JPA join doubling
                .collect(Collectors.toList());

        // Net balance per person (negative = owes money, positive = is owed money).
        Map<String, Double> balances = new HashMap<>();
        Map<String, String> nameByPhone = new HashMap<>();

        for (Debt d : pending) {
            String debtorPhone = d.getDebtor().getPhoneNumber().trim();
            String creditorPhone = d.getCreditor().getPhoneNumber().trim();
            if (debtorPhone.equals(creditorPhone)) continue; // ignore any self-debt

            nameByPhone.put(debtorPhone, d.getDebtor().getName());
            nameByPhone.put(creditorPhone, d.getCreditor().getName());

            balances.merge(debtorPhone, -d.getAmount(), Double::sum);
            balances.merge(creditorPhone, d.getAmount(), Double::sum);
        }

        // Smallest (most negative) balance first for debtors; largest first for creditors.
        PriorityQueue<PersonBalance> debtors = new PriorityQueue<>(Comparator.comparingDouble(pb -> pb.balance));
        PriorityQueue<PersonBalance> creditors = new PriorityQueue<>((a, b) -> Double.compare(b.balance, a.balance));

        for (Map.Entry<String, Double> e : balances.entrySet()) {
            double bal = e.getValue();
            if (Math.abs(bal) < 0.01) continue; // already square -> drops out
            PersonBalance pb = new PersonBalance(nameByPhone.get(e.getKey()), e.getKey(), bal);
            if (bal < 0) {
                debtors.add(pb);
            } else {
                creditors.add(pb);
            }
        }

        List<SettlementResponse> results = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            PersonBalance debtor = debtors.poll();
            PersonBalance creditor = creditors.poll();

            double amount = Math.min(-debtor.balance, creditor.balance);
            amount = Math.round(amount * 100.0) / 100.0;

            results.add(new SettlementResponse(
                    debtor.name, debtor.phone,
                    creditor.name, creditor.phone,
                    amount));

            debtor.balance += amount;
            creditor.balance -= amount;

            if (debtor.balance < -0.01) debtors.add(debtor);
            if (creditor.balance > 0.01) creditors.add(creditor);
        }

        return results;
    }

    private static class PersonBalance {
        final String name;
        final String phone;
        double balance;

        PersonBalance(String name, String phone, double balance) {
            this.name = name;
            this.phone = phone;
            this.balance = balance;
        }
    }
}
