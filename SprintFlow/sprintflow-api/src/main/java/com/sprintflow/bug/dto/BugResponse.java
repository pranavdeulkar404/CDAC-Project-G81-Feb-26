package com.sprintflow.bug.dto;

import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.bug.entity.BugStatus;
import com.sprintflow.user.dto.UserSummary;

import java.time.Instant;

public record BugResponse(
        Long id,
        String title,
        String description,
        BugSeverity severity,
        BugStatus status,
        Long projectId,
        String projectTitle,
        UserSummary assignedTo,
        UserSummary reportedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
