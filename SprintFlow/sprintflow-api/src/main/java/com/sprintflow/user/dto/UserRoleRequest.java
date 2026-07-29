package com.sprintflow.user.dto;

import com.sprintflow.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleRequest(@NotNull UserRole role) {
}
