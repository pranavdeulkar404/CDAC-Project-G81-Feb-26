package com.sprintflow.user.dto;

import com.sprintflow.user.entity.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean accountEnabled,
        boolean otpVerified,
        Instant createdAt
) {
}
