package com.fairshare.debt_settlement.controller;

import com.fairshare.debt_settlement.dto.SettlementResponse;
import com.fairshare.debt_settlement.service.SettlementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settle")
@CrossOrigin(origins = "*")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping
    public ResponseEntity<List<SettlementResponse>> settle() {
        return ResponseEntity.ok(settlementService.settleDebts());
    }

    @PostMapping("/complete")
    public ResponseEntity<?> completeSettlement(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(java.util.Map.of("error", "User not authenticated"));
        }
        settlementService.completeSettlement(principal.getName());
        return ResponseEntity.ok(java.util.Map.of("message", "Settlement completed and history updated."));
    }
}
