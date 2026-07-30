package com.sprintflow.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AiProviderStatus(
        @Schema(example = "true") boolean enabled,
        @Schema(example = "true") boolean configured,
        @Schema(example = "true") boolean available,
        @Schema(example = "GROQ") String provider,
        @Schema(example = "openai/gpt-oss-20b") String model,
        @Schema(example = "AI assistance is ready") String message
) {
}
