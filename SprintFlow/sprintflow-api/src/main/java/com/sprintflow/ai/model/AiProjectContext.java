package com.sprintflow.ai.model;

import com.sprintflow.project.entity.ProjectStatus;

public record AiProjectContext(
        String title,
        String description,
        ProjectStatus status
) {
}
