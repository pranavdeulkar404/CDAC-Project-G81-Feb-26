package com.sprintflow.ai.exception;

public class AiRateLimitException extends RuntimeException {
    public AiRateLimitException() {
        super("AI request limit reached. Please try again shortly or continue manually.");
    }
}
