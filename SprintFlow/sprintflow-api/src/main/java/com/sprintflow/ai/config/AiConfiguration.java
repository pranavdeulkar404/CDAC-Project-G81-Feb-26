package com.sprintflow.ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.ai.provider.AiGenerationProvider;
import com.sprintflow.ai.provider.DisabledAiGenerationProvider;
import com.sprintflow.ai.provider.GroqAiGenerationProvider;
import com.sprintflow.ai.validation.AiDraftValidator;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    @Bean
    @Conditional(GroqReadyCondition.class)
    ChatClient groqChatClient(AiProperties properties) {
        Duration timeout = Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds()));
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .completionsPath("/chat/completions")
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .maxCompletionTokens(properties.getMaxCompletionTokens())
                .build();

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .retryTemplate(RetryTemplate.builder().maxAttempts(1).fixedBackoff(1).build())
                .build();
        return ChatClient.create(model);
    }

    @Bean
    @Conditional(GroqReadyCondition.class)
    AiGenerationProvider groqAiGenerationProvider(
            ChatClient groqChatClient,
            AiProperties properties,
            AiDraftValidator validator,
            ObjectMapper objectMapper,
            CircuitBreakerRegistry circuitBreakerRegistry,
            BulkheadRegistry bulkheadRegistry
    ) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("sprintflowAi");
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("sprintflowAi");
        return new GroqAiGenerationProvider(
                groqChatClient, properties, validator, objectMapper, circuitBreaker, bulkhead);
    }

    @Bean
    @Conditional(GroqUnavailableCondition.class)
    AiGenerationProvider disabledAiGenerationProvider(AiProperties properties) {
        return new DisabledAiGenerationProvider(properties);
    }
}
