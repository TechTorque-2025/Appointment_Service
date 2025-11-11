package com.techtorque.appointment_service.entity;

public enum AppointmentStatus {
  PENDING,                    // Initial state after booking
  CONFIRMED,                  // After admin assigns employee(s)
  IN_PROGRESS,                // After employee accepts and starts work
  COMPLETED,                  // After employee marks work as complete
  CUSTOMER_CONFIRMED,         // After customer confirms completion (final)
  CANCELLED,                  // Cancelled by customer or admin (final)
  NO_SHOW                     // Customer didn't show up (final)
}