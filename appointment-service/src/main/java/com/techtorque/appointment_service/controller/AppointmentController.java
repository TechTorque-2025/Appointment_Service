package com.techtorque.appointment_service.controller;

import com.techtorque.appointment_service.dto.*;
import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment & Scheduling", description = "Endpoints for managing service appointments.")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AppointmentController {

  private final AppointmentService appointmentService;

  @Operation(summary = "Book a new appointment")
  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<ApiResponse> bookAppointment(
          @Valid @RequestBody AppointmentRequestDto dto,
          @RequestHeader("X-User-Subject") String customerId) {

    Appointment appointment = appointmentService.bookAppointment(dto, customerId);
    AppointmentResponseDto response = mapToResponseDto(appointment);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success("Appointment booked successfully", response));
  }

  @Operation(summary = "List appointments for the current user (customer or employee)")
  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE')")
  public ResponseEntity<ApiResponse> listAppointments(
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles) {

    List<Appointment> appointments;

    // Determine if user is employee or customer
    if (userRoles.contains("EMPLOYEE")) {
      appointments = appointmentService.getAppointmentsForEmployee(userId);
    } else {
      appointments = appointmentService.getAppointmentsForCustomer(userId);
    }

    List<AppointmentResponseDto> response = appointments.stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.success("Appointments retrieved successfully", response));
  }

  @Operation(summary = "Get details for a specific appointment")
  @GetMapping("/{appointmentId}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'ADMIN')")
  public ResponseEntity<ApiResponse> getAppointmentDetails(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles) {

    Appointment appointment = appointmentService
            .getAppointmentDetails(appointmentId, userId, userRoles)
            .orElseThrow(() -> new RuntimeException("Appointment not found or access denied"));

    AppointmentResponseDto response = mapToResponseDto(appointment);
    return ResponseEntity.ok(ApiResponse.success("Appointment retrieved successfully", response));
  }

  @Operation(summary = "Update an appointment's date/time or instructions (customer only)")
  @PutMapping("/{appointmentId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<ApiResponse> updateAppointment(
          @PathVariable String appointmentId,
          @Valid @RequestBody AppointmentUpdateDto dto,
          @RequestHeader("X-User-Subject") String customerId) {

    Appointment appointment = appointmentService.updateAppointment(appointmentId, dto, customerId);
    AppointmentResponseDto response = mapToResponseDto(appointment);

    return ResponseEntity.ok(ApiResponse.success("Appointment updated successfully", response));
  }

  @Operation(summary = "Cancel an appointment (customer only)")
  @DeleteMapping("/{appointmentId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<ApiResponse> cancelAppointment(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String customerId) {

    appointmentService.cancelAppointment(appointmentId, customerId);
    return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully"));
  }

  @Operation(summary = "Update an appointment's status (employee/admin only)")
  @PatchMapping("/{appointmentId}/status")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
  public ResponseEntity<ApiResponse> updateStatus(
          @PathVariable String appointmentId,
          @Valid @RequestBody StatusUpdateDto dto,
          @RequestHeader("X-User-Subject") String employeeId) {

    Appointment appointment = appointmentService.updateAppointmentStatus(
            appointmentId, dto.getNewStatus(), dto.getAssignedEmployeeId() != null ? dto.getAssignedEmployeeId() : employeeId);
    AppointmentResponseDto response = mapToResponseDto(appointment);

    return ResponseEntity.ok(ApiResponse.success("Appointment status updated successfully", response));
  }

  @Operation(summary = "Check for available appointment slots (public endpoint)")
  @GetMapping("/availability")
  @PreAuthorize("permitAll()") // This endpoint is public as per the API design
  @SecurityRequirement(name = "bearerAuth", scopes = {}) // Override class-level security
  public ResponseEntity<ApiResponse> checkAvailability(
          @RequestParam LocalDate date,
          @RequestParam String serviceType,
          @RequestParam int duration) {

    List<AvailabilitySlotDto> availableSlots =
            appointmentService.checkAvailability(date, serviceType, duration);

    return ResponseEntity.ok(ApiResponse.success("Availability checked successfully", availableSlots));
  }

  @Operation(summary = "Get the daily schedule for an employee")
  @GetMapping("/schedule")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<ApiResponse> getEmployeeSchedule(
          @RequestHeader("X-User-Subject") String employeeId,
          @RequestParam LocalDate date) {

    List<Appointment> schedule = appointmentService.getEmployeeSchedule(employeeId, date);
    List<AppointmentResponseDto> response = schedule.stream()
            .map(this::mapToResponseDto)
            .collect(Collectors.toList());

    return ResponseEntity.ok(ApiResponse.success("Schedule retrieved successfully", response));
  }

  // Helper method to map Entity to DTO
  private AppointmentResponseDto mapToResponseDto(Appointment appointment) {
    return AppointmentResponseDto.builder()
            .id(appointment.getId())
            .customerId(appointment.getCustomerId())
            .vehicleId(appointment.getVehicleId())
            .assignedEmployeeId(appointment.getAssignedEmployeeId())
            .serviceType(appointment.getServiceType())
            .requestedDateTime(appointment.getRequestedDateTime())
            .status(appointment.getStatus())
            .specialInstructions(appointment.getSpecialInstructions())
            .createdAt(appointment.getCreatedAt())
            .updatedAt(appointment.getUpdatedAt())
            .build();
  }
}