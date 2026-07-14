package com.fairshare.debt_settlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDebtRequest {
    private String debtorPhone;
    private String creditorPhone;
    private Long debtorId;    // optional: resolve by id (used when a phone is hidden)
    private Long creditorId;  // optional: resolve by id
    private Double amount;
    private String note;
    private Long groupId;     // optional: tag this debt to a group
}
