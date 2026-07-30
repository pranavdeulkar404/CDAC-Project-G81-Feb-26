package com.sprintflow.integration.audit;

import java.util.Map;

public interface AuditEventPublisher {

    void publish(
            AuditEventType eventType,
            String entityType,
            Long entityId,
            Long actorId,
            String actorName,
            String summary,
            Map<String, String> attributes
    );
}
