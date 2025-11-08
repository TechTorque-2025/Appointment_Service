package com.techtorque.appointment_service.exception;

public class AppointmentNotFoundException extends RuntimeException {

  public AppointmentNotFoundException(String message) {
    super(message);
  }

  public AppointmentNotFoundException(String appointmentId, String userId) {
    super(String.format("Appointment with ID '%s' not found for user '%s'", appointmentId, userId));
  }
}
