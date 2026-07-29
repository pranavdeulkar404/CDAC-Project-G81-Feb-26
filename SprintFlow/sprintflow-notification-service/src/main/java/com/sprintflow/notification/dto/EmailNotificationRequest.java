package com.sprintflow.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailNotificationRequest(
        @NotBlank @Email String recipientEmail,
        @NotBlank @Size(max = 120) String recipientName,
        @NotBlank @Size(max = 180) String subject,
        @NotNull EmailNotificationType notificationType,
        @Size(max = 4000) String message,
        @Size(max = 10) String otpCode,
        @Size(max = 180) String projectTitle,
        @Size(max = 180) String taskTitle,
        @Size(max = 180) String bugTitle,
        @Size(max = 120) String performedBy,
        Long referenceId
) {
}
