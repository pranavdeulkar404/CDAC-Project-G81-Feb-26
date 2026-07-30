package com.sprintflow.integration.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class HttpAuditClient implements AuditClient {

    private static final Logger log = LoggerFactory.getLogger(HttpAuditClient.class);
    private final RestClient restClient;

    public HttpAuditClient(RestClient.Builder builder, AuditProperties properties) {
        this.restClient = builder
                .baseUrl(properties.url())
                .build();
    }

    @Override
    public boolean send(AuditEvent event) {
        try {
            restClient.post()
                    .uri("/api/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            log.warn("Audit service could not record {} for {} {}. Main operation continues: {}",
                    event.eventType(), event.entityType(), event.entityId(),
                    exception.getClass().getSimpleName());
            return false;
        }
    }
}
