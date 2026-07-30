package com.sprintflow.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiDraftRequest(
        @NotBlank(message = "Describe the work or issue to generate a draft")
        @Size(min = 5, max = 600, message = "The AI prompt must be between 5 and 600 characters")
        @Schema(example = "Add email OTP verification with expiry and resend protection")
        String prompt,

        @NotNull(message = "Select a project before generating a draft")
        @Schema(example = "1")
        Long projectId
) {
}
