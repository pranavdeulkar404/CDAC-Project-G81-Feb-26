package com.sprintflow.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull Boolean enabled) {
}
