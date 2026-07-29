package com.sprintflow.notification.service;

import com.sprintflow.notification.dto.EmailNotificationRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class EmailTemplateFactory {

    public String createHtml(EmailNotificationRequest request) {
        validateTypeSpecificFields(request);

        String name = escape(request.recipientName());
        String content = switch (request.notificationType()) {
            case REGISTRATION_OTP, REGISTRATION_OTP_RESEND ->
                    otpContent("Verify your SprintFlow account", request.otpCode(),
                            "This code expires in 10 minutes.");
            case PASSWORD_RESET_OTP ->
                    otpContent("Reset your SprintFlow password", request.otpCode(),
                            "This code expires in 10 minutes. If you did not request a reset, you can ignore this email.");
            case TASK_ASSIGNED, TASK_REASSIGNED ->
                    workContent(request.message(), "Task", request.taskTitle(), request.projectTitle(), request.performedBy());
            case TASK_UPDATED ->
                    workContent(request.message(), "Task updated", request.taskTitle(), request.projectTitle(), request.performedBy());
            case BUG_ASSIGNED, BUG_REASSIGNED ->
                    workContent(request.message(), "Bug", request.bugTitle(), request.projectTitle(), request.performedBy());
            case BUG_UPDATED ->
                    workContent(request.message(), "Bug updated", request.bugTitle(), request.projectTitle(), request.performedBy());
            case PROJECT_UPDATED ->
                    workContent(request.message(), "Project updated", request.projectTitle(), null, request.performedBy());
        };

        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                    <tr><td align="center" style="padding:32px 16px">
                      <table role="presentation" width="100%%" style="max-width:620px;background:#fff;border-radius:14px;box-shadow:0 8px 30px rgba(23,32,51,.08)">
                        <tr><td style="padding:28px 32px;border-bottom:1px solid #e8edf5">
                          <span style="font-size:24px;font-weight:700;color:#3b5ccc">SprintFlow</span>
                        </td></tr>
                        <tr><td style="padding:32px">
                          <p style="margin:0 0 20px;font-size:16px">Hello %s,</p>
                          %s
                          <p style="margin:28px 0 0;color:#667085;font-size:13px">This is an automated SprintFlow notification.</p>
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(name, content);
    }

    private String otpContent(String heading, String otp, String note) {
        return """
                <h1 style="font-size:22px;margin:0 0 14px">%s</h1>
                <p style="line-height:1.6;color:#475467">Use the following one-time code:</p>
                <div style="display:inline-block;letter-spacing:8px;font-size:30px;font-weight:700;color:#2947b8;background:#eef2ff;padding:16px 22px;border-radius:10px">%s</div>
                <p style="line-height:1.6;color:#475467">%s</p>
                """.formatted(escape(heading), escape(otp), escape(note));
    }

    private String workContent(String message, String label, String title, String projectTitle, String performedBy) {
        StringBuilder body = new StringBuilder();
        body.append("<h1 style=\"font-size:22px;margin:0 0 14px\">")
                .append(escape(label)).append("</h1>");
        if (title != null) {
            body.append("<p style=\"font-size:17px;font-weight:600;margin:0 0 10px\">")
                    .append(escape(title)).append("</p>");
        }
        if (projectTitle != null) {
            body.append("<p style=\"color:#667085;margin:0 0 16px\">Project: ")
                    .append(escape(projectTitle)).append("</p>");
        }
        body.append("<p style=\"line-height:1.6;color:#475467\">")
                .append(escape(message == null ? "There is an update waiting for you in SprintFlow." : message))
                .append("</p>");
        if (performedBy != null) {
            body.append("<p style=\"color:#667085\">Updated by ")
                    .append(escape(performedBy)).append("</p>");
        }
        return body.toString();
    }

    private void validateTypeSpecificFields(EmailNotificationRequest request) {
        switch (request.notificationType()) {
            case REGISTRATION_OTP, REGISTRATION_OTP_RESEND, PASSWORD_RESET_OTP -> {
                if (request.otpCode() == null || !request.otpCode().matches("\\d{6}")) {
                    throw new IllegalArgumentException("A six-digit OTP is required for this notification type");
                }
            }
            case TASK_ASSIGNED, TASK_REASSIGNED, TASK_UPDATED -> {
                if (request.taskTitle() == null || request.taskTitle().isBlank()) {
                    throw new IllegalArgumentException("Task title is required for task notifications");
                }
            }
            case BUG_ASSIGNED, BUG_REASSIGNED, BUG_UPDATED -> {
                if (request.bugTitle() == null || request.bugTitle().isBlank()) {
                    throw new IllegalArgumentException("Bug title is required for bug notifications");
                }
            }
            case PROJECT_UPDATED -> {
                if (request.projectTitle() == null || request.projectTitle().isBlank()) {
                    throw new IllegalArgumentException("Project title is required for project notifications");
                }
            }
        }
    }

    private String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
