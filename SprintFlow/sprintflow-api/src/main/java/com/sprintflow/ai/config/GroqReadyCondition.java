package com.sprintflow.ai.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class GroqReadyCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        boolean enabled = context.getEnvironment()
                .getProperty("sprintflow.ai.enabled", Boolean.class, true);
        String apiKey = context.getEnvironment().getProperty("sprintflow.ai.api-key", "");
        return enabled && StringUtils.hasText(apiKey);
    }
}
