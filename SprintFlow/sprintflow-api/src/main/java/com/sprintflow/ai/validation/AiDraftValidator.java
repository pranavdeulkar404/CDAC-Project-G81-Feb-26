package com.sprintflow.ai.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprintflow.ai.dto.BugDraftResponse;
import com.sprintflow.ai.dto.TaskDraftResponse;
import com.sprintflow.ai.exception.AiInvalidResponseException;
import com.sprintflow.bug.entity.BugSeverity;
import com.sprintflow.task.entity.TaskPriority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class AiDraftValidator {

    private static final Pattern HTML_TAG = Pattern.compile("<[/!?A-Za-z][^>]*>");
    private final ObjectMapper strictMapper;

    public AiDraftValidator(ObjectMapper objectMapper) {
        this.strictMapper = objectMapper.copy()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public TaskDraftResponse validateTask(String json) {
        RawTaskDraft raw = parse(json, RawTaskDraft.class);
        List<String> reasons = validateCommon(raw.title(), raw.description());
        String description = normalize(raw.description());
        requireHeading(description, "Overview", reasons);
        requireHeading(description, "Acceptance Criteria", reasons);
        requireHeading(description, "Implementation Notes", reasons);
        int criteria = countAcceptanceCriteria(description);
        if (criteria < 3 || criteria > 7) {
            reasons.add("Acceptance Criteria must contain 3 to 7 hyphenated items");
        }
        TaskPriority priority = enumValue(raw.priority(), TaskPriority.class, "priority", reasons);
        rejectIfInvalid(reasons);
        return new TaskDraftResponse(normalize(raw.title()), description, priority);
    }

    public BugDraftResponse validateBug(String json) {
        RawBugDraft raw = parse(json, RawBugDraft.class);
        List<String> reasons = validateCommon(raw.title(), raw.description());
        String description = normalize(raw.description());
        requireHeading(description, "Summary", reasons);
        requireHeading(description, "Steps to Reproduce", reasons);
        requireHeading(description, "Expected Behaviour", reasons);
        requireHeading(description, "Actual Behaviour", reasons);
        requireHeading(description, "Additional Information", reasons);
        BugSeverity severity = enumValue(raw.severity(), BugSeverity.class, "severity", reasons);
        rejectIfInvalid(reasons);
        return new BugDraftResponse(normalize(raw.title()), description, severity);
    }

    private List<String> validateCommon(String title, String description) {
        List<String> reasons = new ArrayList<>();
        validateField(title, "title", 180, reasons);
        validateField(description, "description", 4000, reasons);
        return reasons;
    }

    private void validateField(String value, String name, int maximum, List<String> reasons) {
        if (value == null || value.isBlank()) {
            reasons.add(name + " is required");
            return;
        }
        String normalized = normalize(value);
        if (normalized.length() > maximum) {
            reasons.add(name + " exceeds " + maximum + " characters");
        }
        if (containsUnsafeControl(normalized)) {
            reasons.add(name + " contains unsupported control characters");
        }
        if (HTML_TAG.matcher(normalized).find()) {
            reasons.add(name + " must not contain HTML");
        }
    }

    private void requireHeading(String description, String heading, List<String> reasons) {
        if (description == null || !Pattern.compile(
                "(?m)^" + Pattern.quote(heading) + "\\s*$").matcher(description).find()) {
            reasons.add("description is missing the " + heading + " heading");
        }
    }

    private int countAcceptanceCriteria(String description) {
        if (description == null) {
            return 0;
        }
        int start = description.indexOf("Acceptance Criteria");
        int end = description.indexOf("Implementation Notes");
        if (start < 0) {
            return 0;
        }
        String section = description.substring(
                start + "Acceptance Criteria".length(),
                end > start ? end : description.length());
        return (int) section.lines().filter(line -> line.stripLeading().startsWith("- ")).count();
    }

    private <E extends Enum<E>> E enumValue(
            String value, Class<E> enumType, String field, List<String> reasons
    ) {
        if (value == null) {
            reasons.add(field + " is required");
            return null;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            reasons.add(field + " is not an allowed value");
            return null;
        }
    }

    private <T> T parse(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            throw new AiInvalidResponseException("The provider returned no structured content");
        }
        try {
            return strictMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new AiInvalidResponseException("The provider response did not match the required schema");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private boolean containsUnsafeControl(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current) && current != '\n' && current != '\t') {
                return true;
            }
        }
        return false;
    }

    private void rejectIfInvalid(List<String> reasons) {
        if (!reasons.isEmpty()) {
            throw new AiInvalidResponseException(reasons);
        }
    }

    private record RawTaskDraft(String title, String description, String priority) {
    }

    private record RawBugDraft(String title, String description, String severity) {
    }
}
