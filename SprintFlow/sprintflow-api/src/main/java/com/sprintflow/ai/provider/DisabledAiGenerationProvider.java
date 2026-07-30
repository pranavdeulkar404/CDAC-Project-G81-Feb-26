package com.sprintflow.ai.provider;

import com.sprintflow.ai.config.AiProperties;
import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.exception.AiDisabledException;
import com.sprintflow.ai.model.AiProjectContext;
import org.springframework.util.StringUtils;

public class DisabledAiGenerationProvider implements AiGenerationProvider {

    private final AiProperties properties;

    public DisabledAiGenerationProvider(AiProperties properties) {
        this.properties = properties;
    }

    @Override
    public TaskDraftResponse generateTask(AiDraftRequest request, AiProjectContext projectContext) {
        throw unavailable();
    }

    @Override
    public BugDraftResponse generateBug(AiDraftRequest request, AiProjectContext projectContext) {
        throw unavailable();
    }

    @Override
    public AiProviderStatus getStatus() {
        boolean enabled = properties.isEnabled();
        boolean configured = enabled && StringUtils.hasText(properties.getApiKey());
        String message = enabled
                ? "AI assistance is not configured"
                : "AI assistance is disabled";
        return new AiProviderStatus(
                enabled, configured, false, properties.getProvider(), properties.getModel(), message);
    }

    private AiDisabledException unavailable() {
        return new AiDisabledException(properties.isEnabled()
                ? "AI assistance has not been configured on the server."
                : "AI assistance is disabled. You can continue by completing the form manually.");
    }
}
