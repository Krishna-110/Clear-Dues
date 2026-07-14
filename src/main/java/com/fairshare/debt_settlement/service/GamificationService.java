package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.dto.GamificationResponse;
import com.fairshare.debt_settlement.dto.GamificationResponse.Badge;
import com.fairshare.debt_settlement.model.Debt;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@AllArgsConstructor
public class GamificationService {

    private final DebtRepository debtRepository;
    private final PersonRepository personRepository;

    public GamificationResponse forUser(String email) {
        Person user = personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        List<Debt> myDebts = debtRepository.findAllTransactionsForUser(user.getId());

        int settled = 0;
        int pending = 0;
        Set<String> settledWeeks = new HashSet<>();
        for (Debt d : myDebts) {
            if ("SETTLED".equals(d.getStatus())) {
                settled++;
                if (d.getSettledAt() != null) {
                    settledWeeks.add(weekKey(d.getSettledAt().toLocalDate()));
                }
            } else if ("PENDING".equals(d.getStatus())) {
                pending++;
            }
        }

        int total = settled + pending;
        int progress = total == 0 ? 0 : (int) Math.round(settled * 100.0 / total);
        int streak = currentStreakWeeks(settledWeeks);

        List<Badge> badges = new ArrayList<>();
        badges.add(new Badge("FIRST_SETTLEMENT", "First Settlement",
                "Settle your first debt.", settled >= 1));
        badges.add(new Badge("SETTLED_CIRCLE", "Settled Circle",
                "Clear every pending debt.", settled > 0 && pending == 0));
        badges.add(new Badge("CONSISTENT", "On a Roll",
                "Settle something 3 weeks in a row.", streak >= 3));

        return new GamificationResponse(progress, streak, settled, pending, badges);
    }

    // Consecutive weeks (ending this week) that contain at least one settlement.
    private int currentStreakWeeks(Set<String> settledWeeks) {
        if (settledWeeks.isEmpty()) return 0;
        LocalDate cursor = LocalDate.now();
        int streak = 0;
        while (settledWeeks.contains(weekKey(cursor))) {
            streak++;
            cursor = cursor.minusWeeks(1);
        }
        return streak;
    }

    private String weekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return year + "-" + String.format("%02d", week);
    }
}
