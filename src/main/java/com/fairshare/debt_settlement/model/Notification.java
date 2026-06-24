package com.fairshare.debt_settlement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who should see this notification (their email = stable identity).
    @Column(nullable = false)
    private String recipientEmail;

    // PROPOSED | ACCEPTED | DECLINED | REROUTED | DELETED | RESTORED
    @Column(nullable = false)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    // The debt this is about, if any (lets the app jump to it later).
    private Long relatedDebtId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
