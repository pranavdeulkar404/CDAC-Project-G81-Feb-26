package com.sprintflow.ai.exception;

import java.util.List;

public class AiInvalidResponseException extends RuntimeException {

    private final List<String> validationReasons;

    public AiInvalidResponseException(String reason) {
        this(List.of(reason));
    }

    public AiInvalidResponseException(List<String> validationReasons) {
        super("The AI provider returned an invalid draft");
        this.validationReasons = List.copyOf(validationReasons);
    }

    public List<String> getValidationReasons() {
        return validationReasons;
    }
}
