package com.sprintflow.auth.dto;

import com.sprintflow.user.dto.UserSummary;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        UserSummary user
) {
}
