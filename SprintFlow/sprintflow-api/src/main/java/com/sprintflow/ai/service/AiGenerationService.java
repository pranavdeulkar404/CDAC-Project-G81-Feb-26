package com.sprintflow.ai.service;

import com.sprintflow.ai.config.AiProperties;
import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.model.AiProjectContext;
import com.sprintflow.ai.provider.AiGenerationProvider;
import com.sprintflow.ai.validation.AiInputSanitizer;
import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.service.ProjectService;
import com.sprintflow.user.entity.User;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class AiGenerationService {

    private static final int MAX_PROJECT_DESCRIPTION_CHARACTERS = 1200;

    private final AiGenerationProvider provider;
    private final AiProperties properties;
    private final AiInputSanitizer sanitizer;
    private final ProjectService projectService;
    private final CurrentUserService currentUserService;

    public AiGenerationService(
            AiGenerationProvider provider,
            AiProperties properties,
            AiInputSanitizer sanitizer,
            ProjectService projectService,
            CurrentUserService currentUserService
    ) {
        this.provider = provider;
        this.properties = properties;
        this.sanitizer = sanitizer;
        this.projectService = projectService;
        this.currentUserService = currentUserService;
    }

    @PreAuthorize("isAuthenticated()")
    public AiProviderStatus status() {
        return provider.getStatus();
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public TaskDraftResponse generateTask(AiDraftRequest request) {
        PreparedRequest prepared = prepare(request);
        return provider.generateTask(prepared.request(), prepared.context());
    }

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BugDraftResponse generateBug(AiDraftRequest request) {
        PreparedRequest prepared = prepare(request);
        return provider.generateBug(prepared.request(), prepared.context());
    }

    private PreparedRequest prepare(AiDraftRequest request) {
        String prompt = sanitizer.sanitizePrompt(request.prompt(), properties.getMaxInputCharacters());
        User actor = currentUserService.requireUser();
        Project project = projectService.require(request.projectId());
        projectService.requireView(project, actor);
        AiProjectContext context = new AiProjectContext(
                project.getTitle(),
                sanitizer.sanitizeProjectDescription(
                        project.getDescription(), MAX_PROJECT_DESCRIPTION_CHARACTERS),
                project.getStatus());
        return new PreparedRequest(new AiDraftRequest(prompt, request.projectId()), context);
    }

    private record PreparedRequest(AiDraftRequest request, AiProjectContext context) {
    }
}
