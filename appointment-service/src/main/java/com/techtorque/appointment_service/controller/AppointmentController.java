package com.techtorque.appointment_service.controller;

import com.techtorque.appointment_service.dto.request.*;
import com.techtorque.appointment_service.dto.response.*;
import com.techtorque.appointment_service.entity.AppointmentStatus;
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
import java.time.YearMonth;
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

  @Operation(summary = "List appointments with optional filters")
  @GetMapping
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'ADMIN')")
  public ResponseEntity<List<AppointmentResponseDto>> listAppointments(
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles,
          @RequestParam(required = false) String vehicleId,
          @RequestParam(required = false) AppointmentStatus status,
          @RequestParam(required = false) String fromDate,
          @RequestParam(required = false) String toDate) {

    // If filters are provided, use filtered search
    if (vehicleId != null || status != null || fromDate != null || toDate != null) {
      String customerId = userRoles.contains("CUSTOMER") ? userId : null;
      
      // Parse dates if provided
      LocalDate parsedFromDate = fromDate != null ? LocalDate.parse(fromDate) : null;
      LocalDate parsedToDate = toDate != null ? LocalDate.parse(toDate) : null;
      
      List<AppointmentResponseDto> appointments = appointmentService.getAppointmentsWithFilters(
          customerId, vehicleId, status, parsedFromDate, parsedToDate);
      return ResponseEntity.ok(appointments);
    }

    // Otherwise use role-based default listing
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

  @Operation(summary = "Cancel an appointment (customer, employee, or admin)")
  @DeleteMapping("/{appointmentId}")
  @PreAuthorize("hasAnyRole('CUSTOMER', 'EMPLOYEE', 'ADMIN')")
  public ResponseEntity<Void> cancelAppointment(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String userId,
          @RequestHeader("X-User-Roles") String userRoles) {

    appointmentService.cancelAppointment(appointmentId, userId, userRoles);
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
  @PreAuthorize("permitAll()")
  @SecurityRequirement(name = "bearerAuth", scopes = {})
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

  @Operation(summary = "Get monthly calendar view with appointments (employee/admin only)")
  @GetMapping("/calendar")
  @PreAuthorize("hasAnyRole('EMPLOYEE', 'ADMIN')")
  public ResponseEntity<CalendarResponseDto> getMonthlyCalendar(
          @RequestParam int year,
          @RequestParam int month,
          @RequestHeader("X-User-Roles") String userRoles) {

    YearMonth yearMonth = YearMonth.of(year, month);
    CalendarResponseDto calendar = appointmentService.getMonthlyCalendar(yearMonth, userRoles);
    return ResponseEntity.ok(calendar);
  }

  @Operation(summary = "Assign employees to an appointment (admin only)")
  @PostMapping("/{appointmentId}/assign-employees")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AppointmentResponseDto> assignEmployees(
          @PathVariable String appointmentId,
          @Valid @RequestBody AssignEmployeesRequestDto dto,
          @RequestHeader("X-User-Subject") String adminId) {

    AppointmentResponseDto updated = appointmentService.assignEmployees(appointmentId, dto.getEmployeeIds(), adminId);
    return ResponseEntity.ok(updated);
  }

  @Operation(summary = "Employee accepts vehicle arrival and starts work")
  @PostMapping("/{appointmentId}/accept-vehicle")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<AppointmentResponseDto> acceptVehicleArrival(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String employeeId) {

    AppointmentResponseDto updated = appointmentService.acceptVehicleArrival(appointmentId, employeeId);
    return ResponseEntity.ok(updated);
  }

  @Operation(summary = "Employee marks work as complete")
  @PostMapping("/{appointmentId}/complete")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<AppointmentResponseDto> completeWork(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String employeeId) {

    AppointmentResponseDto updated = appointmentService.completeWork(appointmentId, employeeId);
    return ResponseEntity.ok(updated);
  }

  @Operation(summary = "Clock in to start time tracking for an appointment")
  @PostMapping("/{appointmentId}/clock-in")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<TimeSessionResponse> clockIn(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String employeeId) {

    TimeSessionResponse session = appointmentService.clockIn(appointmentId, employeeId);
    return ResponseEntity.ok(session);
  }

  @Operation(summary = "Clock out to stop time tracking for an appointment")
  @PostMapping("/{appointmentId}/clock-out")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<TimeSessionResponse> clockOut(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String employeeId) {

    TimeSessionResponse session = appointmentService.clockOut(appointmentId, employeeId);
    return ResponseEntity.ok(session);
  }

  @Operation(summary = "Get active time session for an appointment")
  @GetMapping("/{appointmentId}/time-session")
  @PreAuthorize("hasRole('EMPLOYEE')")
  public ResponseEntity<TimeSessionResponse> getActiveTimeSession(
          @PathVariable String appointmentId,
          @RequestHeader("X-User-Subject") String employeeId) {

    TimeSessionResponse session = appointmentService.getActiveTimeSession(appointmentId, employeeId);
    
    if (session == null) {
      return ResponseEntity.noContent().build();
    }
    
    return ResponseEntity.ok(session);
  }
}
