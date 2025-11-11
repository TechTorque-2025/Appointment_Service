package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.dto.request.*;
import com.techtorque.appointment_service.dto.response.*;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

public interface AppointmentService {

  AppointmentResponseDto bookAppointment(AppointmentRequestDto dto, String customerId);

  List<AppointmentResponseDto> getAppointmentsForUser(String userId, String userRoles);

  List<AppointmentResponseDto> getAppointmentsWithFilters(
      String customerId, String vehicleId, AppointmentStatus status, LocalDate fromDate, LocalDate toDate);

  AppointmentResponseDto getAppointmentDetails(String appointmentId, String userId, String userRoles);

  AppointmentResponseDto updateAppointment(String appointmentId, AppointmentUpdateDto dto, String customerId);

  void cancelAppointment(String appointmentId, String userId, String userRoles);

  AppointmentResponseDto updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String employeeId);

  AvailabilityResponseDto checkAvailability(LocalDate date, String serviceType, int duration);

  ScheduleResponseDto getEmployeeSchedule(String employeeId, LocalDate date);

  CalendarResponseDto getMonthlyCalendar(YearMonth month, String userRole);

  AppointmentResponseDto assignEmployees(String appointmentId, Set<String> employeeIds, String adminId);

  AppointmentResponseDto acceptVehicleArrival(String appointmentId, String employeeId);

  AppointmentResponseDto completeWork(String appointmentId, String employeeId);

  // Time tracking methods
  TimeSessionResponse clockIn(String appointmentId, String employeeId);

  TimeSessionResponse clockOut(String appointmentId, String employeeId);

  TimeSessionResponse getActiveTimeSession(String appointmentId, String employeeId);
}