package com.sprintflow.notification.dto;

import com.sprintflow.notification.entity.NotificationType;
import com.sprintflow.notification.entity.ReferenceType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String message,
        NotificationType type,
        ReferenceType referenceType,
        Long referenceId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
