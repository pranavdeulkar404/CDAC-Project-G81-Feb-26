package com.sprintflow.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.ai.config.AiProperties;
import com.sprintflow.ai.dto.AiDraftRequest;
import com.sprintflow.ai.dto.AiProviderStatus;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.exception.AiInvalidResponseException;
import com.sprintflow.ai.exception.AiRateLimitException;
import com.sprintflow.ai.exception.AiUnavailableException;
import com.sprintflow.ai.model.AiProjectContext;
import com.sprintflow.ai.prompt.AiPromptTemplates;
import com.sprintflow.ai.validation.AiDraftValidator;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class GroqAiGenerationProvider implements AiGenerationProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqAiGenerationProvider.class);
    private static final Duration STATUS_FAILURE_WINDOW = Duration.ofSeconds(30);

    private final ChatClient chatClient;
    private final AiProperties properties;
    private final AiDraftValidator validator;
    private final ObjectMapper objectMapper;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;
    private volatile Instant unavailableUntil = Instant.EPOCH;

    public GroqAiGenerationProvider(
            ChatClient chatClient,
            AiProperties properties,
            AiDraftValidator validator,
            ObjectMapper objectMapper,
            CircuitBreaker circuitBreaker,
            Bulkhead bulkhead
    ) {
        this.chatClient = chatClient;
        this.properties = properties;
        this.validator = validator;
        this.objectMapper = objectMapper;
        this.circuitBreaker = circuitBreaker;
        this.bulkhead = bulkhead;
    }

    @Override
    public TaskDraftResponse generateTask(AiDraftRequest request, AiProjectContext projectContext) {
        return generate(
                "task",
                request,
                projectContext,
                AiPromptTemplates.TASK_SYSTEM,
                AiPromptTemplates.taskSchema(),
                validator::validateTask);
    }

    @Override
    public BugDraftResponse generateBug(AiDraftRequest request, AiProjectContext projectContext) {
        return generate(
                "bug",
                request,
                projectContext,
                AiPromptTemplates.BUG_SYSTEM,
                AiPromptTemplates.bugSchema(),
                validator::validateBug);
    }

    @Override
    public AiProviderStatus getStatus() {
        boolean circuitOpen = circuitBreaker.getState() == CircuitBreaker.State.OPEN
                || circuitBreaker.getState() == CircuitBreaker.State.FORCED_OPEN;
        boolean available = !circuitOpen && Instant.now().isAfter(unavailableUntil);
        return new AiProviderStatus(
                true,
                true,
                available,
                properties.getProvider(),
                properties.getModel(),
                available ? "AI assistance is ready" : "AI assistance is temporarily unavailable");
    }

    private <T> T generate(
            String operation,
            AiDraftRequest request,
            AiProjectContext projectContext,
            String systemPrompt,
            Map<String, Object> schema,
            Function<String, T> converter
    ) {
        long started = System.nanoTime();
        Supplier<T> providerCall = () -> generateWithOneCorrection(
                request, projectContext, systemPrompt, schema, converter);
        Supplier<T> circuitProtected = CircuitBreaker.decorateSupplier(circuitBreaker, providerCall);
        Supplier<T> isolated = Bulkhead.decorateSupplier(bulkhead, circuitProtected);

        try {
            T result = isolated.get();
            unavailableUntil = Instant.EPOCH;
            log.info("AI generation completed: operation={}, model={}, durationMs={}",
                    operation, properties.getModel(), elapsedMillis(started));
            return result;
        } catch (BulkheadFullException exception) {
            log.warn("AI generation rejected: operation={}, model={}, category=bulkhead",
                    operation, properties.getModel());
            throw new AiRateLimitException();
        } catch (CallNotPermittedException exception) {
            markUnavailable();
            log.warn("AI generation rejected: operation={}, model={}, category=circuit-open",
                    operation, properties.getModel());
            throw new AiUnavailableException();
        } catch (AiInvalidResponseException exception) {
            markUnavailable();
            log.warn("AI generation failed: operation={}, model={}, category=invalid-output",
                    operation, properties.getModel());
            throw exception;
        } catch (RuntimeException exception) {
            RuntimeException translated = translate(exception);
            markUnavailable();
            log.warn("AI generation failed: operation={}, model={}, category={}",
                    operation, properties.getModel(),
                    translated instanceof AiRateLimitException ? "rate-limit" : "provider-unavailable");
            throw translated;
        }
    }

    private <T> T generateWithOneCorrection(
            AiDraftRequest request,
            AiProjectContext projectContext,
            String systemPrompt,
            Map<String, Object> schema,
            Function<String, T> converter
    ) {
        String userPrompt = AiPromptTemplates.userPrompt(request.prompt(), projectContext);
        try {
            return converter.apply(invoke(systemPrompt, userPrompt, schema));
        } catch (AiInvalidResponseException firstFailure) {
            String correction = systemPrompt
                    + "\n\nCORRECTION REQUIRED: The previous response violated the schema for these reasons: "
                    + safeReasons(firstFailure.getValidationReasons())
                    + ". Return a completely corrected JSON object only.";
            return converter.apply(invoke(correction, userPrompt, schema));
        }
    }

    private String invoke(String systemPrompt, String userPrompt, Map<String, Object> schema) {
        ResponseFormat.JsonSchema jsonSchema = ResponseFormat.JsonSchema.builder()
                .name("sprintflow_draft")
                .schema(schema)
                .strict(true)
                .build();
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(ResponseFormat.Type.JSON_SCHEMA)
                .jsonSchema(jsonSchema)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxCompletionTokens(properties.getMaxCompletionTokens())
                .responseFormat(responseFormat)
                .build();

        String content = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .call()
                .content();
        if (content == null || content.length() > properties.getMaxOutputCharacters()) {
            throw new AiInvalidResponseException("The provider response exceeded the allowed size");
        }
        return content;
    }

    private RuntimeException translate(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof RestClientResponseException responseException) {
                int status = responseException.getStatusCode().value();
                if (status == 429) {
                    return new AiRateLimitException();
                }
                return new AiUnavailableException();
            }
            current = current.getCause();
        }
        return new AiUnavailableException();
    }

    private String safeReasons(List<String> reasons) {
        try {
            String value = objectMapper.writeValueAsString(reasons);
            return value.length() <= 600 ? value : value.substring(0, 600);
        } catch (Exception exception) {
            return "[\"response did not match the required schema\"]";
        }
    }

    private void markUnavailable() {
        unavailableUntil = Instant.now().plus(STATUS_FAILURE_WINDOW);
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
