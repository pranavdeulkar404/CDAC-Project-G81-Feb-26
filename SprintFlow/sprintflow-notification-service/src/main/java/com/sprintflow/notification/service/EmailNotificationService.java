package com.sprintflow.notification.service;

import com.sprintflow.notification.dto.EmailNotificationRequest;
import com.sprintflow.notification.dto.EmailNotificationResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final EmailTemplateFactory templateFactory;
    private final String senderAddress;

    public EmailNotificationService(
            JavaMailSender mailSender,
            EmailTemplateFactory templateFactory,
            @Value("${spring.mail.username}") String senderAddress
    ) {
        this.mailSender = mailSender;
        this.templateFactory = templateFactory;
        this.senderAddress = senderAddress;
    }

    public EmailNotificationResponse send(EmailNotificationRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(senderAddress, "SprintFlow");
            helper.setTo(request.recipientEmail());
            helper.setSubject(request.subject());
            helper.setText(templateFactory.createHtml(request), true);
            mailSender.send(message);
            log.info("Email notification {} delivered to {}", request.notificationType(), masked(request.recipientEmail()));
            return new EmailNotificationResponse(true, "Email delivered", Instant.now());
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException exception) {
            log.warn("Email notification {} could not be delivered to {}: {}",
                    request.notificationType(), masked(request.recipientEmail()), exception.getClass().getSimpleName());
            return new EmailNotificationResponse(false,
                    "Email could not be delivered. The main SprintFlow operation is unaffected.", Instant.now());
        }
    }

    private String masked(String email) {
        int at = email.indexOf('@');
        if (at < 2) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
