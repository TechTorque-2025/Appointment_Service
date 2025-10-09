package com.techtorque.appointment_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/appointments")
@Tag(name = "Appointment & Scheduling", description = "Endpoints for managing service appointments.")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

  // @Autowired
  // private AppointmentService appointmentService;

  @Operation(summary = "Book a new appointment")
  @PostMapping
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<?> bookAppointment(
          // @RequestBody AppointmentRequestDto dto,
          @RequestHeader("X-User-Subject") String customerId) {

    // TODO: Delegate to appointmentService.bookAppointment(dto, customerId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "List appointments for the current user (customer or employee)")
  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE')")
  public ResponseEntity<?> listAppointments(@RequestHeader("X-User-Subject") String userId) {
    // TODO: Service layer will need logic to determine if user is a customer or employee
    // and fetch the appropriate list of appointments.
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Get details for a specific appointment")
  @GetMapping("/{appointmentId}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'ADMIN')")
  public ResponseEntity<?> getAppointmentDetails(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles) {

    // TODO: Delegate to appointmentService.getAppointmentDetails(appointmentId, userId, userRoles);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Update an appointment's date/time or instructions (customer only)")
  @PutMapping("/{appointmentId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<?> updateAppointment(
          @PathVariable String appointmentId,
          // @RequestBody AppointmentUpdateDto dto,
          @RequestHeader("X-User-Subject") String customerId) {

    // TODO: Delegate to appointmentService.updateAppointment(appointmentId, dto, customerId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Cancel an appointment (customer only)")
  @DeleteMapping("/{appointmentId}")
  @PreAuthorize("hasRole('CUSTOMER')")
  public ResponseEntity<?> cancelAppointment(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String customerId) {

    // TODO: Delegate to appointmentService.cancelAppointment(appointmentId, customerId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Update an appointment's status (employee/admin only)")
  @PatchMapping("/{appointmentId}/status")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
  public ResponseEntity<?> updateStatus(
          @PathVariable String appointmentId,
          // @RequestBody StatusUpdateDto dto,
          @RequestHeader("X-User-Subject") String employeeId) {

    // TODO: Delegate to appointmentService.updateAppointmentStatus(appointmentId, dto.getNewStatus(), employeeId);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Check for available appointment slots (public endpoint)")
  @GetMapping("/availability")
  @PreAuthorize("permitAll()") // This endpoint is public as per the API design [cite: 30]
  @SecurityRequirement(name = "bearerAuth", scopes = {}) // Override class-level security
  public ResponseEntity<?> checkAvailability(
          @RequestParam LocalDate date,
          @RequestParam String serviceType,
          @RequestParam int duration) {

    // TODO: Delegate to appointmentService.checkAvailability(date, serviceType, duration);
    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Get the daily schedule for an employee")
  @GetMapping("/schedule")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<?> getEmployeeSchedule(
          @RequestHeader("X-User-Subject") String employeeId,
          @RequestParam LocalDate date) {

    // TODO: Delegate to appointmentService.getEmployeeSchedule(employeeId, date);
    return ResponseEntity.ok().build();
  }
}