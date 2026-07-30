package com.sprintflow.integration.audit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SpringAuditEventPublisherTest {

    @Test
    void publishesAnImmutableEventWithGeneratedMetadata() {
        ApplicationEventPublisher applicationPublisher = mock(ApplicationEventPublisher.class);
        SpringAuditEventPublisher publisher = new SpringAuditEventPublisher(applicationPublisher);

        publisher.publish(
                AuditEventType.TASK_CREATED,
                "TASK",
                20L,
                1L,
                "Manager",
                "Created task \"Build screen\".",
                Map.of("status", "TODO")
        );

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(applicationPublisher).publishEvent(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.eventId()).isNotNull();
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.source()).isEqualTo("sprintflow-api");
        assertThat(event.eventType()).isEqualTo(AuditEventType.TASK_CREATED);
        assertThat(event.attributes()).containsEntry("status", "TODO");
    }
}
