package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import java.util.List;
import java.util.Optional;

public interface AppointmentService {

  Appointment bookAppointment(/* AppointmentRequestDto dto, */ String customerId);

  List<Appointment> getAppointmentsForCustomer(String customerId);

  Optional<Appointment> getAppointmentDetails(String appointmentId, String userId, String userRole);

  Appointment updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String employeeId);

  // Add other method signatures as needed...
}