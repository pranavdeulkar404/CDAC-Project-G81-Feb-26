package com.sprintflow.ai.provider;

import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.model.AiProjectContext;

public interface AiGenerationProvider {

    TaskDraftResponse generateTask(AiDraftRequest request, AiProjectContext projectContext);

    BugDraftResponse generateBug(AiDraftRequest request, AiProjectContext projectContext);

    AiProviderStatus getStatus();
}
