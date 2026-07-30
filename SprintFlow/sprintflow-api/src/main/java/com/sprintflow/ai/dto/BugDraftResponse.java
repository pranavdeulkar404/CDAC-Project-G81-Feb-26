package com.sprintflow.ai.dto;

import com.sprintflow.bug.entity.BugSeverity;
import io.swagger.v3.oas.annotations.media.Schema;

public record BugDraftResponse(
        @Schema(example = "Task Is Not Created After Submitting the Task Form")
        String title,
        @Schema(example = "Summary\nThe task does not appear after submission.\n\nSteps to Reproduce\n1. Open the task form.\n2. Submit valid details.\n\nExpected Behaviour\nThe task appears in the list.\n\nActual Behaviour\nThe form remains open.\n\nAdditional Information\nReproduction details must be confirmed during triage.")
        String description,
        @Schema(example = "HIGH")
        BugSeverity severity
) {
}
