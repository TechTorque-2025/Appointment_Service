package com.techtorque.appointment_service.controller;

import com.techtorque.appointment_service.dto.*;
import com.techtorque.appointment_service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment & Scheduling", description = "Endpoints for managing service appointments.")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

  private final AppointmentService appointmentService;

  public AppointmentController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  @Operation(summary = "Book a new appointment")
  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<AppointmentResponseDto> bookAppointment(
          @Valid @RequestBody AppointmentRequestDto dto,
          @RequestHeader("X-User-Subject") String customerId) {

    AppointmentResponseDto response = appointmentService.bookAppointment(dto, customerId);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "List appointments for the current user (customer or employee)")
  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'ADMIN')")
  public ResponseEntity<List<AppointmentResponseDto>> listAppointments(
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles) {

    List<AppointmentResponseDto> appointments = appointmentService.getAppointmentsForUser(userId, userRoles);
    return ResponseEntity.ok(appointments);
  }

  @Operation(summary = "Get details for a specific appointment")
  @GetMapping("/{appointmentId}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'ADMIN')")
  public ResponseEntity<AppointmentResponseDto> getAppointmentDetails(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles) {

    AppointmentResponseDto appointment = appointmentService.getAppointmentDetails(appointmentId, userId, userRoles);
    return ResponseEntity.ok(appointment);
  }

  @Operation(summary = "Update an appointment's date/time or instructions (customer only)")
  @PutMapping("/{appointmentId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<AppointmentResponseDto> updateAppointment(
          @PathVariable String appointmentId,
          @Valid @RequestBody AppointmentUpdateDto dto,
          @RequestHeader("X-User-Subject") String customerId) {

    AppointmentResponseDto updated = appointmentService.updateAppointment(appointmentId, dto, customerId);
    return ResponseEntity.ok(updated);
  }

  @Operation(summary = "Cancel an appointment (customer only)")
  @DeleteMapping("/{appointmentId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<Void> cancelAppointment(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String customerId) {

    appointmentService.cancelAppointment(appointmentId, customerId);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Update an appointment's status (employee/admin only)")
  @PatchMapping("/{appointmentId}/status")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
  public ResponseEntity<AppointmentResponseDto> updateStatus(
          @PathVariable String appointmentId,
          @Valid @RequestBody StatusUpdateDto dto,
          @RequestHeader("X-User-Subject") String employeeId) {

    AppointmentResponseDto updated = appointmentService.updateAppointmentStatus(
        appointmentId, dto.getNewStatus(), employeeId);
    return ResponseEntity.ok(updated);
  }

  @Operation(summary = "Check for available appointment slots (public endpoint)")
  @GetMapping("/availability")
  @PreAuthorize("permitAll()") // This endpoint is public as per the API design
  @SecurityRequirement(name = "bearerAuth", scopes = {}) // Override class-level security
  public ResponseEntity<AvailabilityResponseDto> checkAvailability(
          @RequestParam LocalDate date,
          @RequestParam String serviceType,
          @RequestParam int duration) {

    AvailabilityResponseDto availability = appointmentService.checkAvailability(date, serviceType, duration);
    return ResponseEntity.ok(availability);
  }

  @Operation(summary = "Get the daily schedule for an employee")
  @GetMapping("/schedule")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<ScheduleResponseDto> getEmployeeSchedule(
          @RequestHeader("X-User-Subject") String employeeId,
          @RequestParam LocalDate date) {

    ScheduleResponseDto schedule = appointmentService.getEmployeeSchedule(employeeId, date);
    return ResponseEntity.ok(schedule);
  }
}