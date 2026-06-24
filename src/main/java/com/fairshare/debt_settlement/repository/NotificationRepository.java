package com.fairshare.debt_settlement.repository;

import com.fairshare.debt_settlement.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailIgnoreCaseOrderByCreatedAtDesc(String recipientEmail);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE LOWER(n.recipientEmail) = LOWER(:email) AND n.read = false")
    void markAllRead(@Param("email") String email);
}
