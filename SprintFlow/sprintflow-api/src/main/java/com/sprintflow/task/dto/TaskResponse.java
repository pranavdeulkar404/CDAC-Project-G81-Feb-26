package com.sprintflow.task.dto;

import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.task.entity.TaskStatus;
import com.sprintflow.user.dto.UserSummary;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        Long projectId,
        String projectTitle,
        UserSummary assignedTo,
        UserSummary createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}
