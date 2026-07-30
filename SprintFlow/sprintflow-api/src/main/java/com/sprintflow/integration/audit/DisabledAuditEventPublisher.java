package com.sprintflow.integration.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "sprintflow.audit.enabled", havingValue = "false")
public class DisabledAuditEventPublisher implements AuditEventPublisher {

    @Override
    public void publish(
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Long actorId,
            String actorName,
            String summary,
            Map<String, String> attributes
    ) {
        // Intentionally disabled by configuration.
    }
}
