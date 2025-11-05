package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.dto.*;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

  AppointmentResponseDto bookAppointment(AppointmentRequestDto dto, String customerId);

  List<AppointmentResponseDto> getAppointmentsForUser(String userId, String userRoles);

  AppointmentResponseDto getAppointmentDetails(String appointmentId, String userId, String userRoles);

  AppointmentResponseDto updateAppointment(String appointmentId, AppointmentUpdateDto dto, String customerId);

  void cancelAppointment(String appointmentId, String customerId);

  AppointmentResponseDto updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String employeeId);

  AvailabilityResponseDto checkAvailability(LocalDate date, String serviceType, int duration);

  ScheduleResponseDto getEmployeeSchedule(String employeeId, LocalDate date);
}