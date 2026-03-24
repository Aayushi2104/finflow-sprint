package com.finflow.application_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {
        log.error("ApiException: {}", ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ErrorResponse.of(ex.getStatus(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("RuntimeException: {}", ex.getMessage());

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.getMessage();

        if (message != null) {
            if (message.toLowerCase().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (message.toLowerCase().contains("access denied")) {
                status = HttpStatus.FORBIDDEN;
            } else if (message.toLowerCase().contains("only draft")) {
                status = HttpStatus.BAD_REQUEST;
            }
        }

        return ResponseEntity
                .status(status)
                .body(ErrorResponse.of(status, message, request.getRequestURI()));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", 400);
        response.put("error", "VALIDATION_FAILED");
        response.put("message", "Input validation failed");
        response.put("fieldErrors", fieldErrors);
        response.put("path", request.getRequestURI());

        log.error("Validation failed: {}", fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.error("AccessDenied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(HttpStatus.FORBIDDEN,
                        "You don't have permission to access this resource",
                        request.getRequestURI()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Something went wrong. Please try again later.",
                        request.getRequestURI()));
    }
}