package com.sprintflow.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "Validation Failed", "One or more fields are invalid",
                request.getRequestURI(), fields);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND, "Not Found", exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ApiError> conflict(ConflictException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Conflict", exception.getMessage(), request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> badRequest(BusinessException exception, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Request Rejected", exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException exception, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Forbidden", exception.getMessage(),
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthorized(AuthenticationException exception, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid email or password",
                request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Conflict",
                "The operation conflicts with existing SprintFlow data", request.getRequestURI(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "The request could not be completed", request.getRequestURI(), Map.of());
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String error, String message, String path, Map<String, String> fields
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), error, message, path, fields));
    }
}
