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
    private String status = "PENDING"; // PENDING or SETTLED

    private java.time.LocalDateTime settledAt;

    private String note;
}
