package com.fairshare.debt_settlement.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "debts")
@Data
@NoArgsConstructor
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne
    @JoinColumn(name = "debtor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // If this person is deleted, delete this debt
    private Person debtor;

    // "To Person" - The one who gets paid
    @ManyToOne
    @JoinColumn(name = "creditor_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // Same here, cascade the delete
    private Person creditor;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String status = "PENDING"; // UNCONFIRMED | PENDING | SETTLED | DECLINED | DELETED

    private java.time.LocalDateTime settledAt;

    private String note;

    // Optional group this debt is tagged to (for per-group views). The settlement engine stays
    // global and ignores this; it is metadata only.
    @ManyToOne
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({"members", "owner"})
    private Group group;

    // Soft-delete audit trail: a deleted debt keeps its row so a mistake is visible/recoverable.
    private java.time.LocalDateTime deletedAt;
    private String deletedBy;
}
