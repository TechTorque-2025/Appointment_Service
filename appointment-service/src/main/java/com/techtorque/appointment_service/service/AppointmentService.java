package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AppointmentService {

  Appointment bookAppointment(/* AppointmentRequestDto dto, */ String customerId);

  List<Appointment> getAppointmentsForCustomer(String customerId);

  Optional<Appointment> getAppointmentDetails(String appointmentId, String userId, String userRole);

  Appointment updateAppointment(String appointmentId, /* AppointmentUpdateDto dto, */ String customerId);

  void cancelAppointment(String appointmentId, String customerId);

  Appointment updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String employeeId);

  Object checkAvailability(LocalDate date, String serviceType, int duration); // Return type can be a DTO

  Object getEmployeeSchedule(String employeeId, LocalDate date); // Return type can be a DTO
}