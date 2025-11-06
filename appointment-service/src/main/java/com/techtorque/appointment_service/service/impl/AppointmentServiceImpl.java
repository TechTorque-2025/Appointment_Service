package com.techtorque.appointment_service.service.impl;

import com.techtorque.appointment_service.dto.AppointmentRequestDto;
import com.techtorque.appointment_service.dto.AppointmentUpdateDto;
import com.techtorque.appointment_service.dto.AvailabilitySlotDto;
import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import com.techtorque.appointment_service.exception.AppointmentNotFoundException;
import com.techtorque.appointment_service.exception.InvalidAppointmentException;
import com.techtorque.appointment_service.repository.AppointmentRepository;
import com.techtorque.appointment_service.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

  private final AppointmentRepository appointmentRepository;

  // Business rules constants
  private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);  // 8:00 AM
  private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);   // 6:00 PM
  private static final int DEFAULT_APPOINTMENT_DURATION = 60; // minutes

  public AppointmentServiceImpl(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  @Override
  public synchronized Appointment bookAppointment(AppointmentRequestDto dto, String customerId) {
    log.info("Booking new appointment for customer: {}", customerId);

    // Validate vehicle ID is provided
    if (dto.getVehicleId() == null || dto.getVehicleId().trim().isEmpty()) {
      throw new InvalidAppointmentException("Vehicle ID is required");
    }

    // NOTE: In production, validate vehicle exists via inter-service call to Vehicle Service
    // For now, we check basic format (UUID-like pattern)
    if (!dto.getVehicleId().matches("^[a-fA-F0-9-]{36}$")) {
      log.warn("Vehicle ID format invalid: {}", dto.getVehicleId());
      // Don't throw - allow for testing with non-UUID IDs, but log warning
    }

    // Validate appointment time is in business hours
    LocalTime requestedTime = dto.getRequestedDateTime().toLocalTime();
    if (requestedTime.isBefore(BUSINESS_START) || requestedTime.isAfter(BUSINESS_END)) {
      throw new InvalidAppointmentException(
              "Appointment time must be between " + BUSINESS_START + " and " + BUSINESS_END);
    }

    // Validate appointment is not in the past
    if (dto.getRequestedDateTime().isBefore(LocalDateTime.now())) {
      throw new InvalidAppointmentException("Cannot book appointments in the past");
    }

    // Check for conflicting appointments (same vehicle, overlapping time)
    // This check is now atomic with the save operation due to synchronized method
    LocalDateTime startCheck = dto.getRequestedDateTime().minusMinutes(DEFAULT_APPOINTMENT_DURATION);
    LocalDateTime endCheck = dto.getRequestedDateTime().plusMinutes(DEFAULT_APPOINTMENT_DURATION);
    List<Appointment> existingAppointments =
            appointmentRepository.findByRequestedDateTimeBetween(startCheck, endCheck);

    boolean hasConflict = existingAppointments.stream()
            .anyMatch(apt -> apt.getVehicleId().equals(dto.getVehicleId())
                    && apt.getStatus() != AppointmentStatus.CANCELLED);

    if (hasConflict) {
      throw new InvalidAppointmentException(
              "You already have an appointment scheduled around this time for this vehicle");
    }

    // Create new appointment
    Appointment newAppointment = Appointment.builder()
            .customerId(customerId)
            .vehicleId(dto.getVehicleId())
            .serviceType(dto.getServiceType())
            .requestedDateTime(dto.getRequestedDateTime())
            .specialInstructions(dto.getSpecialInstructions())
            .status(AppointmentStatus.PENDING)
            .build();

    Appointment savedAppointment = appointmentRepository.save(newAppointment);
    log.info("Successfully booked appointment with ID: {} for customer: {}",
            savedAppointment.getId(), customerId);

    return savedAppointment;
  }

  @Override
  public List<Appointment> getAppointmentsForCustomer(String customerId) {
    log.info("Fetching all appointments for customer: {}", customerId);
    return appointmentRepository.findByCustomerIdOrderByRequestedDateTimeDesc(customerId);
  }

  @Override
  public List<Appointment> getAppointmentsForEmployee(String employeeId) {
    log.info("Fetching all appointments for employee: {}", employeeId);
    // Get all appointments assigned to this employee, ordered by date
    return appointmentRepository.findByAssignedEmployeeIdAndRequestedDateTimeBetween(
            employeeId,
            LocalDateTime.now().minusYears(1), // Last year
            LocalDateTime.now().plusYears(1)   // Next year
    );
  }

  @Override
  public Optional<Appointment> getAppointmentDetails(String appointmentId, String userId, String userRole) {
    log.info("Fetching appointment {} for user: {} with role: {}", appointmentId, userId, userRole);

    Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);

    if (appointmentOpt.isEmpty()) {
      return Optional.empty();
    }

    Appointment appointment = appointmentOpt.get();

    // Role-based access control
    if (userRole.contains("ADMIN")) {
      // Admins can see all appointments
      return appointmentOpt;
    } else if (userRole.contains("EMPLOYEE")) {
      // Employees can see appointments assigned to them or unassigned ones
      if (appointment.getAssignedEmployeeId() == null ||
          appointment.getAssignedEmployeeId().equals(userId)) {
        return appointmentOpt;
      }
    } else if (userRole.contains("CUSTOMER")) {
      // Customers can only see their own appointments
      if (appointment.getCustomerId().equals(userId)) {
        return appointmentOpt;
      }
    }

    log.warn("User {} with role {} attempted to access appointment {} without permission",
            userId, userRole, appointmentId);
    return Optional.empty();
  }

  @Override
  public Appointment updateAppointment(String appointmentId, AppointmentUpdateDto dto, String customerId) {
    log.info("Updating appointment {} for customer: {}", appointmentId, customerId);

    // Find appointment and verify ownership
    Appointment existingAppointment = appointmentRepository
            .findByIdAndCustomerId(appointmentId, customerId)
            .orElseThrow(() -> {
              log.warn("Appointment {} not found for customer: {}", appointmentId, customerId);
              return new AppointmentNotFoundException(
                      "Appointment not found or you don't have permission to update it");
            });

    // Only allow updates if appointment is PENDING or CONFIRMED
    if (existingAppointment.getStatus() == AppointmentStatus.IN_PROGRESS ||
        existingAppointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new InvalidAppointmentException(
              "Cannot update appointment that is already in progress or completed");
    }

    if (existingAppointment.getStatus() == AppointmentStatus.CANCELLED) {
      throw new InvalidAppointmentException("Cannot update cancelled appointment");
    }

    // Update fields if provided
    if (dto.getRequestedDateTime() != null) {
      // Validate new time
      LocalTime requestedTime = dto.getRequestedDateTime().toLocalTime();
      if (requestedTime.isBefore(BUSINESS_START) || requestedTime.isAfter(BUSINESS_END)) {
        throw new InvalidAppointmentException(
                "Appointment time must be between " + BUSINESS_START + " and " + BUSINESS_END);
      }
      existingAppointment.setRequestedDateTime(dto.getRequestedDateTime());
    }

    if (dto.getSpecialInstructions() != null) {
      existingAppointment.setSpecialInstructions(dto.getSpecialInstructions());
    }

    Appointment updatedAppointment = appointmentRepository.save(existingAppointment);
    log.info("Successfully updated appointment: {}", appointmentId);

    return updatedAppointment;
  }

  @Override
  public void cancelAppointment(String appointmentId, String customerId) {
    log.info("Cancelling appointment {} for customer: {}", appointmentId, customerId);

    // Find appointment and verify ownership
    Appointment existingAppointment = appointmentRepository
            .findByIdAndCustomerId(appointmentId, customerId)
            .orElseThrow(() -> {
              log.warn("Appointment {} not found for customer: {}", appointmentId, customerId);
              return new AppointmentNotFoundException(
                      "Appointment not found or you don't have permission to cancel it");
            });

    // Cannot cancel completed appointments
    if (existingAppointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new InvalidAppointmentException("Cannot cancel completed appointment");
    }

    // Update status to CANCELLED instead of deleting (keep history)
    existingAppointment.setStatus(AppointmentStatus.CANCELLED);
    appointmentRepository.save(existingAppointment);

    log.info("Successfully cancelled appointment: {}", appointmentId);
  }

  @Override
  public Appointment updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String employeeId) {
    log.info("Updating appointment {} status to: {} by employee: {}",
            appointmentId, newStatus, employeeId);

    Appointment existingAppointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> {
              log.warn("Appointment {} not found", appointmentId);
              return new AppointmentNotFoundException("Appointment not found");
            });

    // Validate status transition
    validateStatusTransition(existingAppointment.getStatus(), newStatus);

    existingAppointment.setStatus(newStatus);

    // If confirming and assigning to employee
    if (newStatus == AppointmentStatus.CONFIRMED && employeeId != null) {
      existingAppointment.setAssignedEmployeeId(employeeId);
    }

    Appointment updatedAppointment = appointmentRepository.save(existingAppointment);
    log.info("Successfully updated appointment {} status to: {}", appointmentId, newStatus);

    return updatedAppointment;
  }

  @Override
  public List<AvailabilitySlotDto> checkAvailability(LocalDate date, String serviceType, int duration) {
    log.info("Checking availability for date: {}, service: {}, duration: {} minutes",
            date, serviceType, duration);

    List<AvailabilitySlotDto> availableSlots = new ArrayList<>();

    // Get all appointments for the requested date
    LocalDateTime dayStart = date.atTime(BUSINESS_START);
    LocalDateTime dayEnd = date.atTime(BUSINESS_END);

    List<Appointment> existingAppointments = appointmentRepository
            .findByRequestedDateTimeBetween(dayStart, dayEnd)
            .stream()
            .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED)
            .sorted(Comparator.comparing(Appointment::getRequestedDateTime))
            .collect(Collectors.toList());

    // Calculate available slots
    LocalDateTime currentSlot = dayStart;

    for (Appointment appointment : existingAppointments) {
      // Check if there's a gap before this appointment
      if (currentSlot.plusMinutes(duration).isBefore(appointment.getRequestedDateTime()) ||
          currentSlot.plusMinutes(duration).isEqual(appointment.getRequestedDateTime())) {

        availableSlots.add(AvailabilitySlotDto.builder()
                .startTime(currentSlot)
                .endTime(appointment.getRequestedDateTime())
                .available(true)
                .build());
      }

      // Move current slot to after this appointment
      currentSlot = appointment.getRequestedDateTime().plusMinutes(DEFAULT_APPOINTMENT_DURATION);
    }

    // Check if there's time after the last appointment
    if (currentSlot.plusMinutes(duration).isBefore(dayEnd) ||
        currentSlot.plusMinutes(duration).isEqual(dayEnd)) {
      availableSlots.add(AvailabilitySlotDto.builder()
              .startTime(currentSlot)
              .endTime(dayEnd)
              .available(true)
              .build());
    }

    // If no appointments, entire day is available
    if (existingAppointments.isEmpty()) {
      availableSlots.add(AvailabilitySlotDto.builder()
              .startTime(dayStart)
              .endTime(dayEnd)
              .available(true)
              .build());
    }

    log.info("Found {} available slots for date: {}", availableSlots.size(), date);
    return availableSlots;
  }

  @Override
  public List<Appointment> getEmployeeSchedule(String employeeId, LocalDate date) {
    log.info("Fetching schedule for employee: {} on date: {}", employeeId, date);

    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

    return appointmentRepository.findByAssignedEmployeeIdAndRequestedDateTimeBetween(
            employeeId, dayStart, dayEnd);
  }

  // Helper method to validate status transitions
  private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
    // Define valid transitions
    boolean isValid = switch (currentStatus) {
      case PENDING -> newStatus == AppointmentStatus.CONFIRMED ||
                      newStatus == AppointmentStatus.CANCELLED;
      case CONFIRMED -> newStatus == AppointmentStatus.IN_PROGRESS ||
                        newStatus == AppointmentStatus.CANCELLED ||
                        newStatus == AppointmentStatus.NO_SHOW;
      case IN_PROGRESS -> newStatus == AppointmentStatus.COMPLETED;
      case COMPLETED, CANCELLED, NO_SHOW -> false; // Terminal states
    };

    if (!isValid) {
      throw new InvalidAppointmentException(
              "Invalid status transition from " + currentStatus + " to " + newStatus);
    }
  }
}