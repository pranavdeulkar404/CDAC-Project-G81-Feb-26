package com.sprintflow.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.ai.config.AiProperties;
import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.exception.AiInvalidResponseException;
import com.sprintflow.ai.model.AiProjectContext;
import com.sprintflow.ai.validation.AiDraftValidator;
import com.sprintflow.project.entity.ProjectStatus;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GroqAiGenerationProviderTest {

    ChatClient chatClient;
    ChatClient.ChatClientRequestSpec requestSpec;
    ChatClient.CallResponseSpec responseSpec;
    GroqAiGenerationProvider provider;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.options(any(OpenAiChatOptions.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        AiProperties properties = new AiProperties();
        ObjectMapper objectMapper = new ObjectMapper();
        provider = new GroqAiGenerationProvider(
                chatClient,
                properties,
                new AiDraftValidator(objectMapper),
                objectMapper,
                CircuitBreaker.ofDefaults("test-ai"),
                Bulkhead.ofDefaults("test-ai"));
    }

    @Test
    void correctsAnInvalidTaskResponseAtMostOnce() {
        when(responseSpec.content())
                .thenReturn("{}")
                .thenReturn(validTaskJson());

        var draft = provider.generateTask(request(), context());

        assertThat(draft.title()).isEqualTo("Implement OTP Verification");
        verify(responseSpec, times(2)).content();
    }

    @Test
    void failsAfterTheSingleCorrectiveAttempt() {
        when(responseSpec.content()).thenReturn("{}", "{}", validTaskJson());

        assertThatThrownBy(() -> provider.generateTask(request(), context()))
                .isInstanceOf(AiInvalidResponseException.class);
        verify(responseSpec, times(2)).content();
    }

    private AiDraftRequest request() {
        return new AiDraftRequest("Add OTP verification", 10L);
    }

    private AiProjectContext context() {
        return new AiProjectContext("Portal", "Customer portal", ProjectStatus.ACTIVE);
    }

    private String validTaskJson() {
        return """
                {
                  "title": "Implement OTP Verification",
                  "description": "Overview\\nAdd OTP verification.\\n\\nAcceptance Criteria\\n- Send an OTP.\\n- Enforce expiry.\\n- Add resend cooldown.\\n\\nImplementation Notes\\n- Reuse authentication.",
                  "priority": "HIGH"
                }
                """;
    }
}
