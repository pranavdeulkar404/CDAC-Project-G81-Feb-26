package com.sprintflow.integration.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "sprintflow.audit")
public record AuditProperties(
        boolean enabled,
        String url,
        Duration connectTimeout,
        Duration readTimeout
) {
}
