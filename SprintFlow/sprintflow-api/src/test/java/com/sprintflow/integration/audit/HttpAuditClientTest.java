package com.sprintflow.integration.audit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpAuditClientTest {

    @Test
    void postsAuditEventToCollector() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAuditClient client = new HttpAuditClient(builder, properties());
        server.expect(once(), requestTo("http://localhost:8082/api/audit-events"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        boolean delivered = client.send(event());

        assertThat(delivered).isTrue();
        server.verify();
    }

    @Test
    void isolatesCollectorFailureFromMainOperation() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        HttpAuditClient client = new HttpAuditClient(builder, properties());
        server.expect(once(), requestTo("http://localhost:8082/api/audit-events"))
                .andRespond(withServerError());

        boolean delivered = client.send(event());

        assertThat(delivered).isFalse();
        server.verify();
    }

    private AuditProperties properties() {
        return new AuditProperties(
                true,
                "http://localhost:8082",
                Duration.ofSeconds(1),
                Duration.ofSeconds(2)
        );
    }

    private AuditEvent event() {
        return new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                AuditEventType.TASK_CREATED,
                "sprintflow-api",
                "TASK",
                20L,
                1L,
                "Manager",
                "Created task.",
                Map.of("status", "TODO")
        );
    }
}
