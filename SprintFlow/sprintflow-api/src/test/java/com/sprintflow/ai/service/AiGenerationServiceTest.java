package com.sprintflow.ai.service;

import com.sprintflow.ai.config.AiProperties;
import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.provider.AiGenerationProvider;
import com.sprintflow.ai.validation.AiInputSanitizer;
import com.sprintflow.auth.security.CurrentUserService;
import com.sprintflow.project.entity.Project;
import com.sprintflow.project.entity.ProjectStatus;
import com.sprintflow.project.service.ProjectService;
import com.sprintflow.task.entity.TaskPriority;
import com.sprintflow.user.entity.User;
import com.sprintflow.user.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiGenerationServiceTest {

    @Mock AiGenerationProvider provider;
    @Mock ProjectService projectService;
    @Mock CurrentUserService currentUserService;
    AiGenerationService service;
    Project project;
    User manager;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        service = new AiGenerationService(
                provider, properties, new AiInputSanitizer(), projectService, currentUserService);
        manager = new User();
        manager.setId(1L);
        manager.setRole(UserRole.MANAGER);
        project = new Project();
        project.setId(10L);
        project.setTitle("Customer Portal");
        project.setDescription("A customer self-service portal.");
        project.setStatus(ProjectStatus.ACTIVE);
    }

    @Test
    void generatesTaskUsingOnlySanitizedMinimalProjectContext() {
        when(currentUserService.requireUser()).thenReturn(manager);
        when(projectService.require(10L)).thenReturn(project);
        when(provider.generateTask(any(), any())).thenReturn(
                new TaskDraftResponse("Implement OTP", "Overview\nDraft", TaskPriority.HIGH));

        service.generateTask(new AiDraftRequest("  Add OTP verification  ", 10L));

        ArgumentCaptor<AiDraftRequest> request = ArgumentCaptor.forClass(AiDraftRequest.class);
        var context = ArgumentCaptor.forClass(com.sprintflow.ai.model.AiProjectContext.class);
        verify(provider).generateTask(request.capture(), context.capture());
        verify(projectService).requireView(project, manager);
        assertThat(request.getValue().prompt()).isEqualTo("Add OTP verification");
        assertThat(request.getValue().projectId()).isEqualTo(10L);
        assertThat(context.getValue().title()).isEqualTo("Customer Portal");
        assertThat(context.getValue().description()).isEqualTo("A customer self-service portal.");
        assertThat(context.getValue().status()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void delegatesStatusWithoutCallingTheModel() {
        AiProviderStatus expected = new AiProviderStatus(
                true, false, false, "GROQ", "openai/gpt-oss-20b", "AI assistance is not configured");
        when(provider.getStatus()).thenReturn(expected);

        assertThat(service.status()).isEqualTo(expected);
        verifyNoMoreInteractions(provider);
    }
}
