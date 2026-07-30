package com.sprintflow.integration.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(
        name = "sprintflow.audit.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SpringAuditEventPublisher implements AuditEventPublisher {

    private static final String SOURCE = "sprintflow-api";
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringAuditEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

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
        applicationEventPublisher.publishEvent(new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                SOURCE,
                entityType,
                entityId,
                actorId,
                actorName,
                summary,
                attributes
        ));
    }
}
