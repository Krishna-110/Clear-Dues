package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.CreateDebtRequest;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class DebtService {
    private final PersonRepository personRepository;
    private final DebtRepository debtRepository;
    private final SmsService smsService;

    public Debt createDebt(CreateDebtRequest request) {
        String debtorPhone = normalizePhone(request.getDebtorPhone());
        String creditorPhone = normalizePhone(request.getCreditorPhone());

        Person debtor = personRepository.findByPhoneNumber(debtorPhone)
                .orElseThrow(() -> new RuntimeException("Debtor not found with phone: " + request.getDebtorPhone()));
        Person creditor = personRepository.findByPhoneNumber(creditorPhone)
                .orElseThrow(() -> new RuntimeException("Creditor not found with phone: " + request.getCreditorPhone()));

        Debt debt = new Debt();
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(request.getAmount());

        Debt savedDebt = debtRepository.save(debt);
        // ... (SMS logic remains same)

        // SMS Notification logic
        try {
            if (debtor.getPhoneNumber() != null && !debtor.getPhoneNumber().isEmpty()) {
                boolean isRegistered = !debtor.getEmail().endsWith("@cleardues.local") && !debtor.getEmail().endsWith("@fairshare.local");
                smsService.sendDebtNotification(
                        debtor.getPhoneNumber(),
                        creditor.getName(),
                        String.valueOf(savedDebt.getAmount()),
                        isRegistered,
                        debtor.getName()
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to send SMS notification: " + e.getMessage());
        }

        return savedDebt;
    }

    public List<Debt> getAllDebts() {
        return debtRepository.findAll();
    }

    public Debt updateDebt(Long id, CreateDebtRequest request) {
        if (id == null) throw new IllegalArgumentException("ID must not be null");
        Debt debt = debtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Debt record not found"));

        String debtorPhone = normalizePhone(request.getDebtorPhone());
        String creditorPhone = normalizePhone(request.getCreditorPhone());

        Person debtor = personRepository.findByPhoneNumber(debtorPhone)
                .orElseThrow(() -> new RuntimeException("Debtor not found with phone: " + request.getDebtorPhone()));
        Person creditor = personRepository.findByPhoneNumber(creditorPhone)
                .orElseThrow(() -> new RuntimeException("Creditor not found with phone: " + request.getCreditorPhone()));

        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(request.getAmount());

        return debtRepository.save(debt);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isEmpty()) return null;
        String cleaned = phone.replaceAll("\\D", ""); // Remove non-digits
        if (cleaned.length() > 10 && (cleaned.startsWith("91"))) {
            return cleaned.substring(cleaned.length() - 10);
        }
        return cleaned;
    }

    public void deleteDebt(Long id) {
        if (id == null) throw new IllegalArgumentException("ID must not be null");
        debtRepository.deleteById(id);
    }

    public Double getTotalOwed(Long userId) {
        return debtRepository.getTotalOwedByUser(userId);
    }

    public Double getTotalReceivable(Long userId) {
        return debtRepository.getTotalOwedToUser(userId);
    }
}
