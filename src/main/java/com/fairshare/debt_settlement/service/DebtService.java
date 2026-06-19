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

        // Record the raw debt first.
        Debt result = saveNewPendingDebt(debtor, creditor, request.getAmount(), request.getNote());

        // Then simplify: collapse chains, net pairs and clear cycles. If this debt got folded
        // into a simplified balance it will be marked SETTLED here (so we don't SMS for it).
        Set<Long> settledIds = simplifyConnectedComponents();

        if (result.getId() == null || !settledIds.contains(result.getId())) {
            // Still a standalone new obligation -> tell the debtor they owe money.
            notifyDebtor(result);
        } else {
            result.setStatus("SETTLED"); // keep the returned object consistent
        }

        return result;
    }

    /**
     * Simplifies the whole pending ledger, one connected group of people at a time.
     *
     * For each group it computes the minimal set of "who pays whom" (greedy min-cash-flow)
     * and, if that differs from what's currently on the books, REWRITES it: the old debts are
     * marked SETTLED (so middlemen drop out and chains collapse) and the minimal direct debts
     * are created fresh. Groups that are already minimal are left untouched.
     *
     * Returns the ids of debts that were settled by this pass.
     */
    @org.springframework.transaction.annotation.Transactional
    public Set<Long> simplifyConnectedComponents() {
        List<Debt> pending = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> d.getDebtor() != null && d.getCreditor() != null)
                .filter(d -> d.getDebtor().getId() != null && d.getCreditor().getId() != null)
                .collect(Collectors.toList());
        if (pending.isEmpty()) return Collections.emptySet();

        // Group people (and their debts) into connected components.
        Map<Long, Long> parent = new HashMap<>();
        Map<Long, Person> personById = new HashMap<>();
        for (Debt d : pending) {
            union(parent, d.getDebtor().getId(), d.getCreditor().getId());
            personById.put(d.getDebtor().getId(), d.getDebtor());
            personById.put(d.getCreditor().getId(), d.getCreditor());
        }
        Map<Long, List<Debt>> byComponent = new HashMap<>();
        for (Debt d : pending) {
            byComponent.computeIfAbsent(find(parent, d.getDebtor().getId()), k -> new ArrayList<>()).add(d);
        }

        Set<Long> settledIds = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (List<Debt> component : byComponent.values()) {
            Map<Long, Double> net = new HashMap<>();
            for (Debt d : component) {
                net.merge(d.getDebtor().getId(), -d.getAmount(), Double::sum);
                net.merge(d.getCreditor().getId(), d.getAmount(), Double::sum);
            }

            List<Transfer> optimized = minCashFlow(net);

            // Already in minimal form -> nothing to do (avoids needless churn / history noise).
            if (sameEdges(component, optimized)) continue;

            // Settle the old rows...
            for (Debt d : component) {
                d.setStatus("SETTLED");
                d.setSettledAt(now);
                debtRepository.save(d);
                settledIds.add(d.getId());
            }
            // ...and create the minimal direct debts.
            for (Transfer t : optimized) {
                Debt nd = new Debt();
                nd.setDebtor(personById.get(t.debtorId));
                nd.setCreditor(personById.get(t.creditorId));
                nd.setAmount(round(t.amount));
                nd.setNote("Simplified");
                debtRepository.save(nd);
            }
        }
        return settledIds;
    }

    // Greedy min-cash-flow: the fewest transfers that settle the given net balances.
    // Deterministic (ties broken by person id) so an already-minimal ledger stays stable.
    private List<Transfer> minCashFlow(Map<Long, Double> net) {
        PriorityQueue<Bal> debtors = new PriorityQueue<>((x, y) -> {
            int c = Double.compare(x.amount, y.amount); // most negative first
            return c != 0 ? c : Long.compare(x.id, y.id);
        });
        PriorityQueue<Bal> creditors = new PriorityQueue<>((x, y) -> {
            int c = Double.compare(y.amount, x.amount); // most positive first
            return c != 0 ? c : Long.compare(x.id, y.id);
        });

        for (Map.Entry<Long, Double> e : net.entrySet()) {
            if (Math.abs(e.getValue()) < 0.01) continue;
            if (e.getValue() < 0) {
                debtors.add(new Bal(e.getKey(), e.getValue()));
            } else {
                creditors.add(new Bal(e.getKey(), e.getValue()));
            }
        }

        List<Transfer> result = new ArrayList<>();
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            Bal d = debtors.poll();
            Bal c = creditors.poll();
            double amount = round(Math.min(-d.amount, c.amount));
            result.add(new Transfer(d.id, c.id, amount));
            d.amount += amount;
            c.amount -= amount;
            if (d.amount < -0.01) debtors.add(d);
            if (c.amount > 0.01) creditors.add(c);
        }
        return result;
    }

    // True when the existing debts already equal the optimized transfers (same edges & amounts).
    private boolean sameEdges(List<Debt> current, List<Transfer> optimized) {
        Map<String, Integer> a = new HashMap<>();
        for (Debt d : current) {
            a.merge(edgeKey(d.getDebtor().getId(), d.getCreditor().getId(), d.getAmount()), 1, Integer::sum);
        }
        Map<String, Integer> b = new HashMap<>();
        for (Transfer t : optimized) {
            b.merge(edgeKey(t.debtorId, t.creditorId, t.amount), 1, Integer::sum);
        }
        return a.equals(b);
    }

    private String edgeKey(Long debtorId, Long creditorId, double amount) {
        return debtorId + ">" + creditorId + ":" + Math.round(amount * 100.0);
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

    private static class Transfer {
        final Long debtorId;
        final Long creditorId;
        final double amount;

        Transfer(Long debtorId, Long creditorId, double amount) {
            this.debtorId = debtorId;
            this.creditorId = creditorId;
            this.amount = amount;
        }
    }

    private static class Bal {
        final Long id;
        double amount;

        Bal(Long id, double amount) {
            this.id = id;
            this.amount = amount;
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
