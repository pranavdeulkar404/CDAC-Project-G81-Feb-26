package com.sprintflow.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.notification.dto.EmailNotificationResponse;
import com.sprintflow.notification.service.EmailNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailNotificationController.class)
class EmailNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    @Test
    void rejectsInvalidEmailRequest() throws Exception {
        mockMvc.perform(post("/api/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipientEmail":"invalid","recipientName":"","subject":"","notificationType":"REGISTRATION_OTP"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.recipientEmail").exists());
    }

    @Test
    void returnsOkWhenEmailIsDelivered() throws Exception {
        when(emailNotificationService.send(any()))
                .thenReturn(new EmailNotificationResponse(true, "Email delivered", Instant.now()));

        mockMvc.perform(post("/api/emails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientEmail":"member@example.com",
                                  "recipientName":"Member",
                                  "subject":"Verify account",
                                  "notificationType":"REGISTRATION_OTP",
                                  "otpCode":"123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivered").value(true));
    }
}
