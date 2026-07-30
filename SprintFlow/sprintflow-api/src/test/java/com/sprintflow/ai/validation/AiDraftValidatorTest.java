package com.sprintflow.ai.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.ai.exception.AiInvalidResponseException;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.task.entity.TaskPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiDraftValidatorTest {

    private AiDraftValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AiDraftValidator(new ObjectMapper());
    }

    @Test
    void validatesAndNormalizesTaskDraft() {
        var draft = validator.validateTask("""
                {
                  "title": "Implement OTP Verification",
                  "description": "Overview\\r\\nAdd secure OTP verification.\\r\\n\\r\\nAcceptance Criteria\\r\\n- Send an OTP.\\r\\n- Enforce expiry.\\r\\n- Enforce resend cooldown.\\r\\n\\r\\nImplementation Notes\\r\\n- Keep durations configurable.",
                  "priority": "HIGH"
                }
                """);

        assertThat(draft.title()).isEqualTo("Implement OTP Verification");
        assertThat(draft.description()).contains("Acceptance Criteria\n- Send an OTP.");
        assertThat(draft.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void rejectsInvalidTaskFieldsAndClassification() {
        assertInvalidTask("", validTaskDescription(), "MEDIUM");
        assertInvalidTask("x".repeat(181), validTaskDescription(), "MEDIUM");
        assertInvalidTask("Valid title", "", "MEDIUM");
        assertInvalidTask("Valid title", validTaskDescription(), "URGENT");
        assertInvalidTask("Valid title", "Overview\nOnly an overview", "MEDIUM");
    }

    @Test
    void rejectsUnexpectedFieldsThatCouldAlterTaskState() {
        assertThatThrownBy(() -> validator.validateTask("""
                {
                  "title": "Implement OTP Verification",
                  "description": "Overview\\nAdd OTP.\\n\\nAcceptance Criteria\\n- Send an OTP.\\n- Enforce expiry.\\n- Add cooldown.\\n\\nImplementation Notes\\n- Reuse authentication.",
                  "priority": "HIGH",
                  "projectId": 99,
                  "assignedToId": 4,
                  "status": "COMPLETED",
                  "dueDate": "2026-08-01"
                }
                """))
                .isInstanceOf(AiInvalidResponseException.class);
    }

    @Test
    void validatesBugDraftWithoutInventedReproductionDetails() {
        var draft = validator.validateBug("""
                {
                  "title": "Task Is Not Created After Form Submission",
                  "description": "Summary\\nThe task does not appear after submission.\\n\\nSteps to Reproduce\\nReproduction details must be confirmed during triage.\\n\\nExpected Behaviour\\nThe task should appear in the list.\\n\\nActual Behaviour\\nThe task is not visible.\\n\\nAdditional Information\\nNo error details were provided.",
                  "severity": "HIGH"
                }
                """);

        assertThat(draft.severity()).isEqualTo(BugSeverity.HIGH);
        assertThat(draft.description()).contains("Reproduction details must be confirmed during triage.");
    }

    @Test
    void rejectsInvalidBugFieldsSeverityHtmlAndProtectedFields() {
        assertThatThrownBy(() -> validator.validateBug("""
                {"title":"Valid title","description":"<script>alert(1)</script>","severity":"SEVERE"}
                """)).isInstanceOf(AiInvalidResponseException.class);

        assertThatThrownBy(() -> validator.validateBug("""
                {
                  "title": "Task Is Not Created",
                  "description": "Summary\\nFailure.\\n\\nSteps to Reproduce\\nTo confirm.\\n\\nExpected Behaviour\\nSaved.\\n\\nActual Behaviour\\nNot visible.\\n\\nAdditional Information\\nNone.",
                  "severity": "HIGH",
                  "status": "CLOSED",
                  "projectId": 3
                }
                """)).isInstanceOf(AiInvalidResponseException.class);
    }

    private void assertInvalidTask(String title, String description, String priority) {
        String json = """
                {"title":%s,"description":%s,"priority":%s}
                """.formatted(json(title), json(description), json(priority));
        assertThatThrownBy(() -> validator.validateTask(json))
                .isInstanceOf(AiInvalidResponseException.class);
    }

    private String validTaskDescription() {
        return """
                Overview
                Add the feature.

                Acceptance Criteria
                - First criterion.
                - Second criterion.
                - Third criterion.

                Implementation Notes
                - To be confirmed.
                """;
    }

    private String json(String value) {
        try {
            return new ObjectMapper().writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
