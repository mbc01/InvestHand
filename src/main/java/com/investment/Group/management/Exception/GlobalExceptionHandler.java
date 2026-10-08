package com.investment.Group.management.Exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions into meaningful HTTP responses.
 *
 * <p>Without this, every failure surfaced as a 500 with a stack trace,
 * including ordinary client mistakes such as an unknown member id or a
 * duplicate monthly payment. Clients could not tell "the server is broken"
 * apart from "you sent something invalid".
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleMemberNotFound(MemberNotFoundException ex) {
        // Expected, user-facing condition: log at WARN, not as a stack trace.
        log.warn("Member lookup failed: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), "/api/contributions");
    }

    @ExceptionHandler(DuplicateContributionException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateContributionException ex) {
        log.warn("Duplicate contribution rejected: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, ex.getMessage(), "/api/contributions");
    }

    /**
     * Bean-validation failures on {@code @Valid @RequestBody} payloads.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Request validation failed");

        return build(HttpStatus.BAD_REQUEST, detail, "/api/contributions");
    }

    /**
     * Malformed JSON, or a value that cannot be bound to the target type
     * (for example a non-numeric id, or text where a number is expected).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body", "/api/contributions");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), "/api/contributions");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }
}