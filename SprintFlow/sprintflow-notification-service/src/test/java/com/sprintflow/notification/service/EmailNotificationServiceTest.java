package com.sprintflow.notification.service;

import com.sprintflow.notification.dto.EmailNotificationRequest;
import com.sprintflow.notification.dto.EmailNotificationType;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailNotificationServiceTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final EmailTemplateFactory templateFactory = new EmailTemplateFactory();

    @Test
    void reportsSmtpFailureWithoutThrowing() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        org.mockito.Mockito.doThrow(new MailSendException("SMTP unavailable")).when(mailSender).send(any(MimeMessage.class));

        EmailNotificationService service =
                new EmailNotificationService(mailSender, templateFactory, "sender@example.com");
        var result = service.send(request());

        assertThat(result.delivered()).isFalse();
        assertThat(result.message()).contains("unaffected");
    }

    @Test
    void templateRejectsOtpNotificationWithoutSixDigitCode() {
        var invalid = new EmailNotificationRequest(
                "member@example.com", "Member", "OTP", EmailNotificationType.REGISTRATION_OTP,
                null, "12", null, null, null, null, null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> templateFactory.createHtml(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("six-digit");
    }

    private EmailNotificationRequest request() {
        return new EmailNotificationRequest(
                "member@example.com", "Member", "Verify", EmailNotificationType.REGISTRATION_OTP,
                null, "123456", null, null, null, null, null);
    }
}
