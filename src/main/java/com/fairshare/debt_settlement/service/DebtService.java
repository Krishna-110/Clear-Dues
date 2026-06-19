package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.CreateDebtRequest;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DebtService {
    private final PersonRepository personRepository;
    private final DebtRepository debtRepository;
    private final SmsService smsService;

    @org.springframework.transaction.annotation.Transactional
    public Debt createDebt(CreateDebtRequest request) {
        String debtorPhone = normalizePhone(request.getDebtorPhone());
        String creditorPhone = normalizePhone(request.getCreditorPhone());

        Person debtor = personRepository.findByPhoneNumber(debtorPhone)
                .orElseThrow(() -> new RuntimeException("Debtor not found with phone: " + request.getDebtorPhone()));
        Person creditor = personRepository.findByPhoneNumber(creditorPhone)
                .orElseThrow(() -> new RuntimeException("Creditor not found with phone: " + request.getCreditorPhone()));

        // Find existing PENDING debts this payment repays: debts in the OPPOSITE
        // direction (where the new creditor was the one who owed the new debtor).
        List<Debt> opposingDebts = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> d.getDebtor() != null && d.getCreditor() != null)
                .filter(d -> d.getDebtor().getId().equals(creditor.getId())
                          && d.getCreditor().getId().equals(debtor.getId()))
                .sorted(Comparator.comparing(Debt::getId)) // settle oldest first
                .collect(Collectors.toList());

        // No matching debt -> behave exactly like before: new pending debt + SMS.
        if (opposingDebts.isEmpty()) {
            return saveNewPendingDebt(debtor, creditor, request.getAmount(), request.getNote(), true);
        }

        // A repayment matched -> settle the existing debt(s) instead of stacking clutter.
        double remaining = request.getAmount();
        LocalDateTime now = LocalDateTime.now();
        Debt lastTouched = null;
        for (Debt d : opposingDebts) {
            if (remaining <= 0.0) break;
            if (remaining >= d.getAmount()) {
                // Fully repaid -> mark this debt SETTLED.
                remaining -= d.getAmount();
                d.setStatus("SETTLED");
                d.setSettledAt(now);
                d.setNote("Settled by repayment from " + creditor.getName());
            } else {
                // Partial repayment -> shrink the outstanding debt.
                d.setAmount(d.getAmount() - remaining);
                remaining = 0.0;
            }
            lastTouched = debtRepository.save(d);
        }

        // Overpaid -> the extra flips the balance the other way as a new pending debt.
        if (remaining > 0.0) {
            return saveNewPendingDebt(debtor, creditor, remaining, request.getNote(), false);
        }

        return lastTouched;
    }

    // Creates a standard PENDING debt and (optionally) fires the SMS notification.
    private Debt saveNewPendingDebt(Person debtor, Person creditor, Double amount, String note, boolean notify) {
        Debt debt = new Debt();
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(amount);
        debt.setNote(note);

        Debt savedDebt = debtRepository.save(debt);

        if (notify) {
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
