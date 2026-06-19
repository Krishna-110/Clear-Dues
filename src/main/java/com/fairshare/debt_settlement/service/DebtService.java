package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.CreateDebtRequest;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

        double amount = request.getAmount();

        // Every PENDING debt between these two people, in either direction.
        List<Debt> pairDebts = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> d.getDebtor() != null && d.getCreditor() != null)
                .filter(d -> isSamePair(d, debtor, creditor))
                .collect(Collectors.toList());

        // Does the creditor currently owe the debtor anything? If so, this entry is a
        // repayment/netting against that balance rather than a brand-new expense.
        boolean hasOpposing = pairDebts.stream().anyMatch(
                d -> d.getDebtor().getId().equals(creditor.getId())
                  && d.getCreditor().getId().equals(debtor.getId()));

        // Pure new expense (nothing owed the other way) -> just record it and notify.
        if (!hasOpposing) {
            return saveNewPendingDebt(debtor, creditor, amount, request.getNote(), true);
        }

        // Netting event: collapse the WHOLE relationship into a single net balance so the
        // pair never carries stacked/contradictory rows. "net" = how much `debtor` owes
        // `creditor` once this entry and every existing pending debt are combined.
        double net = amount;
        for (Debt d : pairDebts) {
            if (d.getDebtor().getId().equals(debtor.getId())) {
                net += d.getAmount(); // debtor already owed creditor
            } else {
                net -= d.getAmount(); // creditor owed debtor
            }
        }

        // Wipe the old rows; they're now folded into the net.
        LocalDateTime now = LocalDateTime.now();
        Debt lastSettled = null;
        for (Debt d : pairDebts) {
            d.setStatus("SETTLED");
            d.setSettledAt(now);
            lastSettled = debtRepository.save(d);
        }

        net = round(net);
        if (Math.abs(net) < 0.01) {
            // Fully squared - nothing outstanding between them.
            return lastSettled;
        }
        if (net > 0) {
            return saveNewPendingDebt(debtor, creditor, net, request.getNote(), false);
        }
        // Balance flipped: the creditor now owes the debtor the leftover.
        return saveNewPendingDebt(creditor, debtor, -net, request.getNote(), false);
    }

    // True when the debt is between exactly these two people, regardless of direction.
    private boolean isSamePair(Debt d, Person p1, Person p2) {
        Long a = d.getDebtor().getId();
        Long b = d.getCreditor().getId();
        return (a.equals(p1.getId()) && b.equals(p2.getId()))
            || (a.equals(p2.getId()) && b.equals(p1.getId()));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
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
