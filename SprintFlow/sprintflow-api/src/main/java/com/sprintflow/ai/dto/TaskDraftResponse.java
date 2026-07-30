package com.sprintflow.ai.dto;

import com.sprintflow.task.entity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;

public record TaskDraftResponse(
        @Schema(example = "Implement OTP Registration Verification with Resend Cooldown")
        String title,
        @Schema(example = "Overview\nImplement OTP-based verification during registration.\n\nAcceptance Criteria\n- An OTP is sent after registration.\n- The OTP expires safely.\n- Resend requests observe a cooldown.")
        String description,
        @Schema(example = "HIGH")
        TaskPriority priority
) {
}
