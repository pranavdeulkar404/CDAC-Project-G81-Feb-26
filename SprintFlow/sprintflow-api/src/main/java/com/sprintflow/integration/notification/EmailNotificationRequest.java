package com.sprintflow.integration.notification;

public record EmailNotificationRequest(
        String recipientEmail,
        String recipientName,
        String subject,
        EmailNotificationType notificationType,
        String message,
        String otpCode,
        String projectTitle,
        String taskTitle,
        String bugTitle,
        String performedBy,
        Long referenceId
) {
}
