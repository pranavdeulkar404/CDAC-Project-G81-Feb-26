package com.sprintflow.ai.validation;

import com.sprintflow.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiInputSanitizerTest {

    private final AiInputSanitizer sanitizer = new AiInputSanitizer();

    @Test
    void trimsAndNormalizesValidInput() {
        assertThat(sanitizer.sanitizePrompt("  Build OTP\r\nverification  ", 600))
                .isEqualTo("Build OTP\nverification");
    }

    @Test
    void rejectsBlankShortAndOversizedInput() {
        assertThatThrownBy(() -> sanitizer.sanitizePrompt("   ", 600))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> sanitizer.sanitizePrompt("four", 600))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> sanitizer.sanitizePrompt("x".repeat(601), 600))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsUnsafeControlCharacters() {
        assertThatThrownBy(() -> sanitizer.sanitizePrompt("Build\u0000feature", 600))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("control characters");
    }

    @Test
    void capsExcessiveRepeatedCharacters() {
        String sanitized = sanitizer.sanitizePrompt("Fix " + "!".repeat(80), 600);
        assertThat(sanitized).isEqualTo("Fix " + "!".repeat(20));
    }
}
