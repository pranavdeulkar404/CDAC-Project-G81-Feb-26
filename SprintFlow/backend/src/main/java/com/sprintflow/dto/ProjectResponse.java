package com.sprintflow.project.dto;

import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.user.dto.UserSummary;

import java.time.Instant;
import java.time.LocalDate;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status,
        UserSummary createdBy,
        long taskCount,
        long bugCount,
        Instant createdAt,
        Instant updatedAt
) {
}
