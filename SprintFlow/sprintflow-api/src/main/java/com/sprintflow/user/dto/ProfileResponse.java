package com.sprintflow.user.dto;

public record ProfileResponse(
        Long userId,
        String name,
        String email,
        String initials,
        String phone,
        String designation,
        String bio
) {
}
