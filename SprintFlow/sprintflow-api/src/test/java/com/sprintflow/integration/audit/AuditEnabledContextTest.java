package com.sprintflow.integration.audit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "sprintflow.audit.enabled=true")
class AuditEnabledContextTest {

    @Autowired
    private AuditEventPublisher auditEventPublisher;

    @Autowired
    private AuditClient auditClient;

    @Autowired
    @Qualifier("auditTaskExecutor")
    private Executor auditTaskExecutor;

    @Test
    void enabledConfigurationProvidesPublisherClientAndDedicatedExecutor() {
        assertThat(auditEventPublisher).isInstanceOf(SpringAuditEventPublisher.class);
        assertThat(auditClient).isInstanceOf(HttpAuditClient.class);
        assertThat(auditTaskExecutor).isNotNull();
    }
}
