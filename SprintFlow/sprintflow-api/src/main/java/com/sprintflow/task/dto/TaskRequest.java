package com.sprintflow.task.dto;

import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.task.entity.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TaskRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull TaskPriority priority,
        @NotNull TaskStatus status,
        LocalDate dueDate,
        @NotNull Long projectId,
        Long assignedToId
) {
}
