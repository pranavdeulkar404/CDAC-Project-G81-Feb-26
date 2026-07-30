package com.sprintflow.ai.controller;

import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.service.AiGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@RestController
@RequestMapping("/api/ai")
@SecurityRequirement(name = "bearerAuth")
public class AiGenerationController {

    private final AiGenerationService aiGenerationService;

    public AiGenerationController(AiGenerationService aiGenerationService) {
        this.aiGenerationService = aiGenerationService;
    }

    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get optional AI drafting availability")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current feature status",
                    content = @Content(schema = @Schema(implementation = AiProviderStatus.class),
                            examples = @ExampleObject(value = """
                                    {"enabled":true,"configured":true,"available":true,
                                    "provider":"GROQ","model":"openai/gpt-oss-20b",
                                    "message":"AI assistance is ready"}"""))),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid")
    })
    AiProviderStatus status() {
        return aiGenerationService.status();
    }

    @PostMapping("/tasks/generate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Generate a preview-only task draft")
    @GenerationResponses
    TaskDraftResponse generateTask(@Valid @RequestBody AiDraftRequest request) {
        return aiGenerationService.generateTask(request);
    }

    @PostMapping("/bugs/generate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "Generate a preview-only bug-report draft")
    @GenerationResponses
    BugDraftResponse generateBug(@Valid @RequestBody AiDraftRequest request) {
        return aiGenerationService.generateBug(request);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Draft generated for review"),
            @ApiResponse(responseCode = "400", description = "Prompt or project selection is invalid"),
            @ApiResponse(responseCode = "401", description = "JWT is missing or invalid"),
            @ApiResponse(responseCode = "403", description = "Only managers and administrators may generate drafts"),
            @ApiResponse(responseCode = "404", description = "Selected project does not exist"),
            @ApiResponse(responseCode = "429", description = "Provider or concurrency limit reached"),
            @ApiResponse(responseCode = "502", description = "Provider returned unusable structured output"),
            @ApiResponse(responseCode = "503", description = "AI is disabled, unconfigured, timed out, or unavailable")
    })
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    private @interface GenerationResponses {
    }
}
