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
     * Returns the outstanding balance between every pair of people, netted directly.
     *
     * This is intentionally PAIRWISE (no routing through third parties): the amount
     * shown for "A should pay B" is exactly what A owes B once their mutual debts are
     * combined. That way the number on screen always matches what recording a repayment
     * will settle.
     */
    public List<SettlementResponse> settleDebts() {
        Set<Long> seen = new HashSet<>();
        List<Debt> pending = debtRepository.findAll().stream()
                .filter(d -> "PENDING".equals(d.getStatus()))
                .filter(d -> d.getDebtor() != null && d.getCreditor() != null)
                .filter(d -> d.getDebtor().getPhoneNumber() != null && d.getCreditor().getPhoneNumber() != null)
                .filter(d -> seen.add(d.getId())) // guard against any JPA join doubling
                .collect(Collectors.toList());

        // Net balance per unordered pair. Key = "loPhone|hiPhone";
        // value = how much loPhone owes hiPhone (may be negative).
        Map<String, Double> pairNet = new HashMap<>();
        Map<String, String> nameByPhone = new HashMap<>();

        for (Debt d : pending) {
            String debtorPhone = d.getDebtor().getPhoneNumber().trim();
            String creditorPhone = d.getCreditor().getPhoneNumber().trim();
            if (debtorPhone.equals(creditorPhone)) continue; // ignore any self-debt

            nameByPhone.put(debtorPhone, d.getDebtor().getName());
            nameByPhone.put(creditorPhone, d.getCreditor().getName());

            // debtor owes creditor d.amount
            if (debtorPhone.compareTo(creditorPhone) < 0) {
                pairNet.merge(debtorPhone + "|" + creditorPhone, d.getAmount(), Double::sum);
            } else {
                pairNet.merge(creditorPhone + "|" + debtorPhone, -d.getAmount(), Double::sum);
            }
        }

        List<SettlementResponse> results = new ArrayList<>();
        for (Map.Entry<String, Double> entry : pairNet.entrySet()) {
            double net = entry.getValue();
            if (Math.abs(net) < 0.01) continue; // squared up

            String[] pair = entry.getKey().split("\\|");
            String lo = pair[0];
            String hi = pair[1];

            String fromPhone;
            String toPhone;
            double amount;
            if (net > 0) {
                fromPhone = lo;  // lo owes hi
                toPhone = hi;
                amount = net;
            } else {
                fromPhone = hi;  // hi owes lo
                toPhone = lo;
                amount = -net;
            }

            results.add(new SettlementResponse(
                    nameByPhone.get(fromPhone), fromPhone,
                    nameByPhone.get(toPhone), toPhone,
                    Math.round(amount * 100.0) / 100.0
            ));
        }

        return results;
    }
}
