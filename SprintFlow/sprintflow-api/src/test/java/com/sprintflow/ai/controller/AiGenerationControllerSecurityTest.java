package com.sprintflow.ai.controller;

import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.exception.AiInvalidResponseException;
import com.sprintflow.ai.exception.AiRateLimitException;
import com.sprintflow.ai.exception.AiUnavailableException;
import com.sprintflow.ai.service.AiGenerationService;
import com.sprintflow.auth.security.CustomUserDetailsService;
import com.sprintflow.auth.security.JwtService;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.task.entity.TaskPriority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiGenerationController.class)
@Import(AiGenerationControllerSecurityTest.MethodSecurityConfiguration.class)
class AiGenerationControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @MockBean AiGenerationService service;
    @MockBean JwtService jwtService;
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JpaMetamodelMappingContext jpaMappingContext;

    @Test
    void unauthenticatedGenerationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/ai/tasks/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void memberGenerationReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/ai/tasks/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanGenerateTaskDraft() throws Exception {
        when(service.generateTask(any())).thenReturn(
                new TaskDraftResponse("Implement OTP", "Overview\nDraft", TaskPriority.HIGH));

        mockMvc.perform(post("/api/ai/tasks/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Implement OTP"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.projectId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorCanGenerateBugDraft() throws Exception {
        when(service.generateBug(any())).thenReturn(
                new BugDraftResponse("Task Is Not Saved", "Summary\nDraft", BugSeverity.HIGH));

        mockMvc.perform(post("/api/ai/bugs/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.severity").value("HIGH"))
                .andExpect(jsonPath("$.assignedToId").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void statusIsAuthenticatedAndDoesNotGenerateContent() throws Exception {
        when(service.status()).thenReturn(new AiProviderStatus(
                true, false, false, "GROQ", "openai/gpt-oss-20b", "AI assistance is not configured"));

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configured").value(false));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void validationAndProviderFailuresUseSafeStatuses() throws Exception {
        mockMvc.perform(post("/api/ai/tasks/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\" \",\"projectId\":null}"))
                .andExpect(status().isBadRequest());

        when(service.generateTask(any()))
                .thenThrow(new AiRateLimitException())
                .thenThrow(new AiInvalidResponseException("invalid"))
                .thenThrow(new AiUnavailableException());

        mockMvc.perform(post("/api/ai/tasks/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson()))
                .andExpect(status().isTooManyRequests());
        mockMvc.perform(post("/api/ai/tasks/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson()))
                .andExpect(status().isBadGateway());
        mockMvc.perform(post("/api/ai/tasks/generate").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(requestJson()))
                .andExpect(status().isServiceUnavailable());
    }

    private String requestJson() {
        return """
                {"prompt":"Add OTP verification with expiry","projectId":1}
                """;
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}
