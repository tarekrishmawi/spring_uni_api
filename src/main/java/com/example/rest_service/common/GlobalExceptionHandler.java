package com.example.rest_service.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {

        HttpStatusCode status = ex.getStatusCode();

        ApiError error = new ApiError(
            LocalDateTime.now(),
            status.value(),
            status.toString(),
            ex.getReason() == null
                ? "Request failed"
                : ex.getReason(),
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> errors =
            ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    field -> field.getField(),
                    field -> field.getDefaultMessage() == null
                        ? "Invalid value"
                        : field.getDefaultMessage(),
                    (first, second) -> first
                ));

        ApiError error = new ApiError(
            LocalDateTime.now(),
            400,
            "Bad Request",
            "Validation failed",
            request.getRequestURI(),
            errors
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        ApiError error = new ApiError(
            LocalDateTime.now(),
            409,
            "Conflict",
            "A record with the same unique value already exists",
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.status(409).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        ApiError error = new ApiError(
            LocalDateTime.now(),
            500,
            "Internal Server Error",
            "An unexpected error occurred",
            request.getRequestURI(),
            Map.of()
        );

        return ResponseEntity.internalServerError().body(error);
    }
}