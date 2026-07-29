package com.sprintflow.notification.dto;

import java.time.Instant;

public record EmailNotificationResponse(
        boolean delivered,
        String message,
        Instant timestamp
) {
}
