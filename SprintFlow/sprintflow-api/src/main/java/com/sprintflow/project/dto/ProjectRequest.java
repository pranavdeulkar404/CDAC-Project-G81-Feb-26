package com.sprintflow.project.dto;

import com.sprintflow.project.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull ProjectStatus status
) {
}
