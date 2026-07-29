package com.sprintflow.bug.dto;

import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BugRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull BugSeverity severity,
        @NotNull BugStatus status,
        @NotNull Long projectId,
        Long assignedToId
) {
}
