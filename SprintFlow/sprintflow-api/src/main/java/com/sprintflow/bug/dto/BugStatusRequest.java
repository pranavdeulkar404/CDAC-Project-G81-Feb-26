package com.sprintflow.bug.dto;

import com.sprintflow.bug.entity.BugStatus;
import jakarta.validation.constraints.NotNull;

public record BugStatusRequest(@NotNull BugStatus status) {
}
