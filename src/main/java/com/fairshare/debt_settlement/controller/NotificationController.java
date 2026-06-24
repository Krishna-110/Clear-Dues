package com.fairshare.debt_settlement.controller;

import com.fairshare.debt_settlement.model.Notification;
import com.fairshare.debt_settlement.service.NotificationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@AllArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> list() {
        return ResponseEntity.ok(notificationService.listFor(currentUserEmail()));
    }

    @PostMapping("/read")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(currentUserEmail());
        return ResponseEntity.noContent().build();
    }

    private String currentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }
}
