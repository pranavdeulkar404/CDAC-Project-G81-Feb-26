package com.sprintflow.comment.dto;

import com.sprintflow.user.dto.UserSummary;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String message,
        UserSummary author,
        Instant createdAt,
        Instant updatedAt
) {
}
