package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.model.Notification;
import com.fairshare.debt_settlement.repository.NotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /** Creates an in-app notification for a recipient. No-op if the recipient has no email. */
    @Transactional
    public Notification notify(String recipientEmail, String type, String message, Long relatedDebtId) {
        if (recipientEmail == null || recipientEmail.isBlank()) return null;
        Notification n = new Notification();
        n.setRecipientEmail(recipientEmail);
        n.setType(type);
        n.setMessage(message);
        n.setRelatedDebtId(relatedDebtId);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(n);
    }

    public List<Notification> listFor(String email) {
        if (email == null) return Collections.emptyList();
        return notificationRepository.findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(email);
    }

    @Transactional
    public void markAllRead(String email) {
        if (email == null) return;
        notificationRepository.markAllRead(email);
    }
}
