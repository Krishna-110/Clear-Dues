package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.CreateDebtRequest;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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

        Debt result;
        boolean newObligation; // candidate for an SMS (genuinely new money owed)

        if (!hasOpposing) {
            // Pure new entry in this direction.
            result = saveNewPendingDebt(debtor, creditor, amount, request.getNote());
            newObligation = true;
        } else {
            // Netting event against a DIRECT balance: collapse the whole pair into a single
            // net row so the pair never carries stacked/contradictory rows. "net" = how much
            // `debtor` owes `creditor` once this entry and every existing pending debt combine.
            double net = amount;
            for (Debt d : pairDebts) {
                if (d.getDebtor().getId().equals(debtor.getId())) {
                    net += d.getAmount(); // debtor already owed creditor
                } else {
                    net -= d.getAmount(); // creditor owed debtor
                }
            }

            LocalDateTime now = LocalDateTime.now();
            Debt lastSettled = null;
            for (Debt d : pairDebts) {
                d.setStatus("SETTLED");
                d.setSettledAt(now);
                lastSettled = debtRepository.save(d);
            }

            net = round(net);
            newObligation = false;
            if (Math.abs(net) < 0.01) {
                result = lastSettled;                                                  // fully squared
            } else if (net > 0) {
                result = saveNewPendingDebt(debtor, creditor, net, request.getNote());
            } else {
                // Balance flipped: the creditor now owes the debtor the leftover.
                result = saveNewPendingDebt(creditor, debtor, -net, request.getNote());
            }
        }

        // Cross out every group of people who are now fully squared. This is what makes a
        // chain settle: recording "Skylar -> Morpheus" zeroes Skylar, Suket and Morpheus,
        // so all of their debts (including this one) are marked SETTLED together.
        Set<Long> settledIds = settleFullyBalancedGroups();

        if (result != null && settledIds.contains(result.getId())) {
            result.setStatus("SETTLED"); // keep the returned object consistent
        } else if (newObligation && result != null && "PENDING".equals(result.getStatus())) {
            // Notify the debtor only for a genuinely new debt that wasn't immediately settled.
            notifyDebtor(result);
        }

        return result;
    }

    /**
     * Marks every PENDING debt as SETTLED when its whole connected group of people is now
     * fully squared (everyone's net balance is ~0). Returns the ids that were settled.
     * Partly-settled groups are left untouched.
     */
    @org.springframework.transaction.annotation.Transactional
    public Set<Long> settleFullyBalancedGroups() {
        List<Debt> pending = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> d.getDebtor() != null && d.getCreditor() != null)
                .collect(Collectors.toList());

        Map<Long, Double> net = new HashMap<>();
        Map<Long, Long> parent = new HashMap<>();
        for (Debt d : pending) {
            Long debtorId = d.getDebtor().getId();
            Long creditorId = d.getCreditor().getId();
            net.merge(debtorId, -d.getAmount(), Double::sum);
            net.merge(creditorId, d.getAmount(), Double::sum);
            union(parent, debtorId, creditorId);
        }

        // A group is settleable only if EVERY member in it is squared.
        Map<Long, Boolean> balancedByRoot = new HashMap<>();
        for (Long person : net.keySet()) {
            balancedByRoot.putIfAbsent(find(parent, person), true);
        }
        for (Map.Entry<Long, Double> e : net.entrySet()) {
            if (Math.abs(e.getValue()) >= 0.01) {
                balancedByRoot.put(find(parent, e.getKey()), false);
            }
        }

        Set<Long> settledIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (Debt d : pending) {
            Long root = find(parent, d.getDebtor().getId());
            if (Boolean.TRUE.equals(balancedByRoot.get(root))) {
                d.setStatus("SETTLED");
                d.setSettledAt(now);
                debtRepository.save(d);
                settledIds.add(d.getId());
            }
        }
        return settledIds;
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

    // --- tiny union-find over person ids, used to find connected groups of people ---
    private Long find(Map<Long, Long> parent, Long x) {
        parent.putIfAbsent(x, x);
        Long root = x;
        while (!parent.get(root).equals(root)) {
            root = parent.get(root);
        }
        Long cur = x;
        while (!parent.get(cur).equals(root)) { // path compression
            Long next = parent.get(cur);
            parent.put(cur, root);
            cur = next;
        }
        return root;
    }

    private void union(Map<Long, Long> parent, Long a, Long b) {
        Long ra = find(parent, a);
        Long rb = find(parent, b);
        if (!ra.equals(rb)) {
            parent.put(ra, rb);
        }
    }

    // Creates a standard PENDING debt (no notification - the caller decides).
    private Debt saveNewPendingDebt(Person debtor, Person creditor, Double amount, String note) {
        Debt debt = new Debt();
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(amount);
        debt.setNote(note);
        return debtRepository.save(debt);
    }

    // Sends the "you owe X" SMS to the debtor of a newly created, still-outstanding debt.
    private void notifyDebtor(Debt debt) {
        try {
            Person debtor = debt.getDebtor();
            if (debtor.getPhoneNumber() != null && !debtor.getPhoneNumber().isEmpty()) {
                boolean isRegistered = !debtor.getEmail().endsWith("@cleardues.local")
                        && !debtor.getEmail().endsWith("@fairshare.local");
                smsService.sendDebtNotification(
                        debtor.getPhoneNumber(),
                        debt.getCreditor().getName(),
                        String.valueOf(debt.getAmount()),
                        isRegistered,
                        debtor.getName()
                );
            }
        } catch (Exception e) {
            // Log error but don't fail the transaction
            System.err.println("Failed to send SMS notification: " + e.getMessage());
        }
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
