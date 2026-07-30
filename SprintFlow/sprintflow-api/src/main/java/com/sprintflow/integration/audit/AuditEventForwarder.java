package com.sprintflow.integration.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(
        name = "sprintflow.audit.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class AuditEventForwarder {

    private final AuditClient auditClient;

    public AuditEventForwarder(AuditClient auditClient) {
        this.auditClient = auditClient;
    }

    @Async("auditTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    public void forward(AuditEvent event) {
        auditClient.send(event);
    }
}
