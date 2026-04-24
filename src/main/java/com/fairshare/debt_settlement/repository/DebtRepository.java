package com.fairshare.debt_settlement.repository;

import com.fairshare.debt_settlement.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DebtRepository extends JpaRepository<Debt, Long> {

    List<Debt> findByDebtorId(Long debtorId);

    List<Debt> findByCreditorId(Long creditorId);

    @Query("SELECT COALESCE(SUM(d.amount), 0.0) FROM Debt d WHERE d.debtor.id = :userId AND d.status = 'PENDING'")
    Double getTotalOwedByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(d.amount), 0.0) FROM Debt d WHERE d.creditor.id = :userId AND d.status = 'PENDING'")
    Double getTotalOwedToUser(@Param("userId") Long userId);

    @Query("SELECT d FROM Debt d WHERE (d.debtor.id = :userId OR d.creditor.id = :userId) AND d.status = 'PENDING'")
    List<Debt> findAllPendingTransactionsForUser(@Param("userId") Long userId);

    @Query("SELECT d FROM Debt d WHERE d.debtor.id = :userId OR d.creditor.id = :userId")
    List<Debt> findAllTransactionsForUser(@Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("DELETE FROM Debt d WHERE (d.debtor.id = :userA AND d.creditor.id = :userB) OR (d.debtor.id = :userB AND d.creditor.id = :userA)")
    void deleteMutualDebts(@Param("userA") Long userA, @Param("userB") Long userB);
}