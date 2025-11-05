package com.techtorque.appointment_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppointmentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleAppointmentNotFound(
      AppointmentNotFoundException ex, HttpServletRequest request) {

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.NOT_FOUND.value())
        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
  }

  @ExceptionHandler(UnauthorizedAccessException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(
      UnauthorizedAccessException ex, HttpServletRequest request) {

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.FORBIDDEN.value())
        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
  }

  @ExceptionHandler(InvalidStatusTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
      InvalidStatusTransitionException ex, HttpServletRequest request) {

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    Map<String, Object> response = new HashMap<>();
    response.put("timestamp", LocalDateTime.now());
    response.put("status", HttpStatus.BAD_REQUEST.value());
    response.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    response.put("message", "Validation failed");
    response.put("errors", errors);
    response.put("path", request.getRequestURI());

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(
      Exception ex, HttpServletRequest request) {

    ErrorResponse errorResponse = ErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .message("An unexpected error occurred: " + ex.getMessage())
        .path(request.getRequestURI())
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
  }
}
