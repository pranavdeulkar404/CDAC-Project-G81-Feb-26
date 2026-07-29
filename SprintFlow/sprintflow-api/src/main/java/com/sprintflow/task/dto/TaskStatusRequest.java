package com.sprintflow.task.dto;

import com.sprintflow.task.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(@NotNull TaskStatus status) {
}
