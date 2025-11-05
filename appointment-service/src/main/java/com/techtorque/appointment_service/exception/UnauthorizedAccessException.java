package com.techtorque.appointment_service.exception;

public class UnauthorizedAccessException extends RuntimeException {

  public UnauthorizedAccessException(String message) {
    super(message);
  }
}
