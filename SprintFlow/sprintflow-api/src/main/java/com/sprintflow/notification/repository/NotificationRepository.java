package com.sprintflow.notification.repository;

import com.sprintflow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    long countByUserIdAndReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.read = true, n.readAt = :readAt where n.user.id = :userId and n.read = false")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
