package com.sprintflow.ai.validation;

import com.sprintflow.common.exception.BusinessException;
import org.springframework.stereotype.Component;

@Component
public class AiInputSanitizer {

    private static final int MAX_REPEATED_CHARACTERS = 20;

    public String sanitizePrompt(String input, int maxCharacters) {
        if (input == null) {
            throw new BusinessException("Describe the work or issue to generate a draft");
        }
        String normalized = normalizeLines(input).trim();
        rejectUnsafeControls(normalized);
        if (normalized.length() < 5 || normalized.length() > maxCharacters) {
            throw new BusinessException("The AI prompt must be between 5 and " + maxCharacters + " characters");
        }
        return limitRepeatedCharacters(normalized);
    }

    public String sanitizeProjectDescription(String description, int maxCharacters) {
        String normalized = normalizeLines(description == null ? "" : description).trim();
        rejectUnsafeControls(normalized);
        normalized = limitRepeatedCharacters(normalized);
        return normalized.length() <= maxCharacters ? normalized : normalized.substring(0, maxCharacters);
    }

    private String normalizeLines(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private void rejectUnsafeControls(String value) {
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current) && current != '\n' && current != '\t') {
                throw new BusinessException("The AI prompt contains unsupported control characters");
            }
        }
    }

    private String limitRepeatedCharacters(String value) {
        StringBuilder result = new StringBuilder(value.length());
        char previous = 0;
        int repeated = 0;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current == previous) {
                repeated++;
            } else {
                previous = current;
                repeated = 1;
            }
            if (repeated <= MAX_REPEATED_CHARACTERS) {
                result.append(current);
            }
        }
        return result.toString();
    }
}
