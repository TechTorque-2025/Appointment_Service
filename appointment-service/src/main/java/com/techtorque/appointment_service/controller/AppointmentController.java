package com.techtorque.appointment_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
          @RequestHeader("X-User-Roles") String userRoles) { // Assuming roles are passed as a comma-separated string

    // TODO: Delegate to appointmentService.getAppointmentDetails(appointmentId, userId, userRoles);
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
}