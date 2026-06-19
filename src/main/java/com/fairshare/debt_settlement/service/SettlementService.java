package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.SettlementResponse;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SettlementService {

    private final DebtRepository debtRepository;
    private final PersonRepository personRepository;

    public SettlementService(DebtRepository debtRepository, PersonRepository personRepository) {
        this.debtRepository = debtRepository;
        this.personRepository = personRepository;
    }

    public List<SettlementResponse> settleDebts() {
        // 1. Fetch only PENDING debts and deduplicate by ID to prevent any JPA/Join doubling
        java.util.Set<Long> processedIds = new java.util.HashSet<>();
        List<Debt> allDebts = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> {
                    if (processedIds.contains(d.getId())) return false;
                    processedIds.add(d.getId());
                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        Map<String, Double> balances = new HashMap<>();

        // 2. Calculate net balances with phone number normalization (trimming)
        for (Debt debt : allDebts) {
            if (debt.getDebtor() == null || debt.getCreditor() == null) continue;
            
            String debtorPhone = debt.getDebtor().getPhoneNumber();
            String creditorPhone = debt.getCreditor().getPhoneNumber();
            
            if (debtorPhone == null || creditorPhone == null) continue;
            
            // Trim to handle any hidden spaces that might cause doubling
            debtorPhone = debtorPhone.trim();
            creditorPhone = creditorPhone.trim();

            balances.put(debtorPhone, balances.getOrDefault(debtorPhone, 0.0) - debt.getAmount());
            balances.put(creditorPhone, balances.getOrDefault(creditorPhone, 0.0) + debt.getAmount());
        }

        // 3. Separate into debtors and creditors (ignoring zero balances)
        PriorityQueue<PersonBalance> debtors = new PriorityQueue<>(Comparator.comparingDouble(pb -> pb.balance));
        PriorityQueue<PersonBalance> creditors = new PriorityQueue<>((pb1, pb2) -> Double.compare(pb2.balance, pb1.balance));

        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            double balance = entry.getValue();
            String phone = entry.getKey();
            
            if (Math.abs(balance) < 0.01) continue;

            Person person = personRepository.findByPhoneNumber(phone).orElse(null);
            if (person != null) {
                if (balance < 0) {
                    debtors.add(new PersonBalance(person.getId(), person.getName(), phone, balance));
                } else {
                    creditors.add(new PersonBalance(person.getId(), person.getName(), phone, balance));
                }
            }
        }

        // 4. Greedy algorithm to simplify debts
        List<SettlementResponse> results = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            PersonBalance debtor = debtors.poll();
            PersonBalance creditor = creditors.poll();

            double amount = Math.min(-debtor.balance, creditor.balance);
            results.add(new SettlementResponse(debtor.name, debtor.idOrPhone, creditor.name, creditor.idOrPhone, amount));

            debtor.balance += amount;
            creditor.balance -= amount;

            if (debtor.balance < -0.01) debtors.add(debtor);
            if (creditor.balance > 0.01) creditors.add(creditor);
        }

        return results;
    }

    private static class PersonBalance {
        String name;
        String idOrPhone; // Now using phone number
        Double balance;

        PersonBalance(Long id, String name, String idOrPhone, Double balance) {
            this.name = name;
            this.idOrPhone = idOrPhone;
            this.balance = balance;
        }
    }
}
