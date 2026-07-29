package com.sprintflow.user.dto;

import com.sprintflow.user.entity.UserRole;

public record UserSummary(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean accountEnabled
) {
}
