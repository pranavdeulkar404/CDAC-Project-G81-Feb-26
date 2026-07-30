package com.sprintflow.integration.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEvent(
        UUID eventId,
        Instant occurredAt,
        AuditEventType eventType,
        String source,
        String entityType,
        Long entityId,
        Long actorId,
        String actorName,
        String summary,
        Map<String, String> attributes
) {
    public AuditEvent {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
