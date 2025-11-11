package com.techtorque.appointment_service.exception;

import com.techtorque.appointment_service.entity.AppointmentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

  public InvalidStatusTransitionException(String message) {
    super(message);
  }

  public InvalidStatusTransitionException(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
    super(String.format("Cannot transition from status '%s' to '%s'", currentStatus, newStatus));
  }

  public InvalidStatusTransitionException(AppointmentStatus currentStatus, AppointmentStatus newStatus, String reason) {
    super(String.format("Cannot transition from status '%s' to '%s': %s", currentStatus, newStatus, reason));
  }
}
