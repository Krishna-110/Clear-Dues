package com.fairshare.debt_settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GamificationResponse {
    private int progressPercent;   // % of your debts that are settled
    private int streakWeeks;       // consecutive weeks with a settlement
    private int settledCount;
    private int pendingCount;
    private List<Badge> badges;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Badge {
        private String key;
        private String name;
        private String description;
        private boolean unlocked;
    }
}
