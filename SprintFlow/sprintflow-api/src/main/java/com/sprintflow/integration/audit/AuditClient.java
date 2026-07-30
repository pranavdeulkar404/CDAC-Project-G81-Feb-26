package com.sprintflow.integration.audit;

public interface AuditClient {

    boolean send(AuditEvent event);
}
