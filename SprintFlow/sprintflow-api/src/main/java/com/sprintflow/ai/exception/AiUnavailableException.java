package com.sprintflow.ai.exception;

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() {
        super("AI assistance is temporarily unavailable. You can continue by completing the form manually.");
    }
}
