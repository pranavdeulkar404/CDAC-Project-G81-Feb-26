package com.sprintflow.integration.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);
    private final RestClient restClient;

    public NotificationClient(RestClient.Builder builder, @Value("${sprintflow.notification-service.url}") String url) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(5000);
        this.restClient = builder.baseUrl(url).requestFactory(requestFactory).build();
    }

    public boolean send(EmailNotificationRequest request) {
        try {
            restClient.post()
                    .uri("/api/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RestClientException exception) {
            log.warn("Notification service could not deliver {}. Main operation continues: {}",
                    request.notificationType(), exception.getClass().getSimpleName());
            return false;
        }
    }
}
