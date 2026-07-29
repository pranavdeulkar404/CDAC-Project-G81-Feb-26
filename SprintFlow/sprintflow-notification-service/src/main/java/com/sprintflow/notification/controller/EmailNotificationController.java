package com.sprintflow.notification.controller;

import com.sprintflow.notification.dto.EmailNotificationRequest;
import com.sprintflow.notification.dto.EmailNotificationResponse;
import com.sprintflow.notification.service.EmailNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
public class EmailNotificationController {

    private final EmailNotificationService emailNotificationService;

    public EmailNotificationController(EmailNotificationService emailNotificationService) {
        this.emailNotificationService = emailNotificationService;
    }

    @PostMapping
    public ResponseEntity<EmailNotificationResponse> send(@Valid @RequestBody EmailNotificationRequest request) {
        EmailNotificationResponse response = emailNotificationService.send(request);
        return ResponseEntity.status(response.delivered() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }
}
