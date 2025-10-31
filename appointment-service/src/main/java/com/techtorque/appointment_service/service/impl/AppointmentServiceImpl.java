package com.techtorque.appointment_service.service.impl;

import com.techtorque.appointment_service.dto.*;
import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import com.techtorque.appointment_service.exception.AppointmentNotFoundException;
import com.techtorque.appointment_service.exception.InvalidStatusTransitionException;
import com.techtorque.appointment_service.exception.UnauthorizedAccessException;
import com.techtorque.appointment_service.repository.AppointmentRepository;
import com.techtorque.appointment_service.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

  private final AppointmentRepository appointmentRepository;

  // Business hours configuration
  private static final LocalTime BUSINESS_START = LocalTime.of(8, 0);
  private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);
  private static final int SLOT_INTERVAL_MINUTES = 30;

  public AppointmentServiceImpl(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  @Override
  public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto, String customerId) {
    log.info("Booking appointment for customer: {}", customerId);

    Appointment appointment = Appointment.builder()
        .customerId(customerId)
        .vehicleId(dto.getVehicleId())
        .serviceType(dto.getServiceType())
        .requestedDateTime(dto.getRequestedDateTime())
        .specialInstructions(dto.getSpecialInstructions())
        .status(AppointmentStatus.PENDING)
        .build();

    Appointment savedAppointment = appointmentRepository.save(appointment);
    log.info("Appointment booked successfully with ID: {}", savedAppointment.getId());

    return convertToDto(savedAppointment);
  }

  @Override
  public List<AppointmentResponseDto> getAppointmentsForUser(String userId, String userRoles) {
    log.info("Fetching appointments for user: {} with roles: {}", userId, userRoles);

    List<Appointment> appointments;

    if (userRoles.contains("ADMIN")) {
      // Admins can see all appointments
      appointments = appointmentRepository.findAll();
    } else if (userRoles.contains("EMPLOYEE")) {
      // Employees see appointments assigned to them
      appointments = appointmentRepository.findByAssignedEmployeeIdAndRequestedDateTimeBetween(
          userId, LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1));
    } else {
      // Customers see their own appointments
      appointments = appointmentRepository.findByCustomerIdOrderByRequestedDateTimeDesc(userId);
    }

    return appointments.stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
  }

  @Override
  public AppointmentResponseDto getAppointmentDetails(String appointmentId, String userId, String userRoles) {
    log.info("Fetching appointment details for ID: {} by user: {}", appointmentId, userId);

    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

    // Check access permissions
    boolean isAdmin = userRoles.contains("ADMIN");
    boolean isAssignedEmployee = userRoles.contains("EMPLOYEE") && userId.equals(appointment.getAssignedEmployeeId());
    boolean isCustomer = userId.equals(appointment.getCustomerId());

    if (!isAdmin && !isAssignedEmployee && !isCustomer) {
      throw new UnauthorizedAccessException("You do not have permission to view this appointment");
    }

    return convertToDto(appointment);
  }

  @Override
  public AppointmentResponseDto updateAppointment(String appointmentId, AppointmentUpdateDto dto, String customerId) {
    log.info("Updating appointment: {} for customer: {}", appointmentId, customerId);

    Appointment appointment = appointmentRepository.findByIdAndCustomerId(appointmentId, customerId)
        .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, customerId));

    // Only allow updates if appointment is PENDING or CONFIRMED
    if (appointment.getStatus() != AppointmentStatus.PENDING &&
        appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new InvalidStatusTransitionException(
          "Cannot update appointment with status: " + appointment.getStatus());
    }

    // Update fields if provided
    if (dto.getRequestedDateTime() != null) {
      appointment.setRequestedDateTime(dto.getRequestedDateTime());
    }
    if (dto.getSpecialInstructions() != null) {
      appointment.setSpecialInstructions(dto.getSpecialInstructions());
    }

    Appointment updatedAppointment = appointmentRepository.save(appointment);
    log.info("Appointment updated successfully: {}", appointmentId);

    return convertToDto(updatedAppointment);
  }

  @Override
  public void cancelAppointment(String appointmentId, String customerId) {
    log.info("Cancelling appointment: {} for customer: {}", appointmentId, customerId);

    Appointment appointment = appointmentRepository.findByIdAndCustomerId(appointmentId, customerId)
        .orElseThrow(() -> new AppointmentNotFoundException(appointmentId, customerId));

    // Only allow cancellation if not already completed or cancelled
    if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
      throw new InvalidStatusTransitionException("Cannot cancel a completed appointment");
    }

    appointment.setStatus(AppointmentStatus.CANCELLED);
    appointmentRepository.save(appointment);

    log.info("Appointment cancelled successfully: {}", appointmentId);
  }

  @Override
  public AppointmentResponseDto updateAppointmentStatus(String appointmentId, AppointmentStatus newStatus, String employeeId) {
    log.info("Updating appointment status: {} to {} by employee: {}", appointmentId, newStatus, employeeId);

    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

    // Validate status transition
    validateStatusTransition(appointment.getStatus(), newStatus);

    // Assign employee if transitioning to CONFIRMED or IN_PROGRESS
    if ((newStatus == AppointmentStatus.CONFIRMED || newStatus == AppointmentStatus.IN_PROGRESS) &&
        appointment.getAssignedEmployeeId() == null) {
      appointment.setAssignedEmployeeId(employeeId);
    }

    appointment.setStatus(newStatus);
    Appointment updatedAppointment = appointmentRepository.save(appointment);

    log.info("Appointment status updated successfully: {}", appointmentId);

    return convertToDto(updatedAppointment);
  }

  @Override
  public AvailabilityResponseDto checkAvailability(LocalDate date, String serviceType, int duration) {
    log.info("Checking availability for date: {}, service: {}, duration: {}", date, serviceType, duration);

    LocalDateTime dayStart = date.atTime(BUSINESS_START);
    LocalDateTime dayEnd = date.atTime(BUSINESS_END);

    // Get all existing appointments for the day
    List<Appointment> existingAppointments = appointmentRepository
        .findByRequestedDateTimeBetween(dayStart, dayEnd);

    // Generate all possible time slots
    List<TimeSlotDto> slots = generateTimeSlots(date, duration, existingAppointments);

    return AvailabilityResponseDto.builder()
        .date(date)
        .serviceType(serviceType)
        .durationMinutes(duration)
        .availableSlots(slots)
        .build();
  }

  @Override
  public ScheduleResponseDto getEmployeeSchedule(String employeeId, LocalDate date) {
    log.info("Fetching schedule for employee: {} on date: {}", employeeId, date);

    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.atTime(23, 59, 59);

    List<Appointment> appointments = appointmentRepository
        .findByAssignedEmployeeIdAndRequestedDateTimeBetween(employeeId, dayStart, dayEnd);

    List<ScheduleItemDto> scheduleItems = appointments.stream()
        .map(this::convertToScheduleItem)
        .collect(Collectors.toList());

    return ScheduleResponseDto.builder()
        .employeeId(employeeId)
        .date(date)
        .appointments(scheduleItems)
        .build();
  }

  // Helper methods
  private AppointmentResponseDto convertToDto(Appointment appointment) {
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

  private ScheduleItemDto convertToScheduleItem(Appointment appointment) {
    return ScheduleItemDto.builder()
        .appointmentId(appointment.getId())
        .customerId(appointment.getCustomerId())
        .vehicleId(appointment.getVehicleId())
        .serviceType(appointment.getServiceType())
        .startTime(appointment.getRequestedDateTime())
        .status(appointment.getStatus())
        .specialInstructions(appointment.getSpecialInstructions())
        .build();
  }

  private List<TimeSlotDto> generateTimeSlots(LocalDate date, int durationMinutes, List<Appointment> existingAppointments) {
    List<TimeSlotDto> slots = new ArrayList<>();
    LocalDateTime currentSlot = date.atTime(BUSINESS_START);
    LocalDateTime endOfDay = date.atTime(BUSINESS_END);

    while (currentSlot.plusMinutes(durationMinutes).isBefore(endOfDay) ||
           currentSlot.plusMinutes(durationMinutes).equals(endOfDay)) {

      LocalDateTime slotEnd = currentSlot.plusMinutes(durationMinutes);
      boolean isAvailable = isSlotAvailable(currentSlot, slotEnd, existingAppointments);

      slots.add(TimeSlotDto.builder()
          .startTime(currentSlot)
          .endTime(slotEnd)
          .available(isAvailable)
          .build());

      currentSlot = currentSlot.plusMinutes(SLOT_INTERVAL_MINUTES);
    }

    return slots;
  }

  private boolean isSlotAvailable(LocalDateTime slotStart, LocalDateTime slotEnd, List<Appointment> existingAppointments) {
    // Check if the slot overlaps with any existing appointment
    for (Appointment appointment : existingAppointments) {
      // Skip cancelled and no-show appointments
      if (appointment.getStatus() == AppointmentStatus.CANCELLED ||
          appointment.getStatus() == AppointmentStatus.NO_SHOW) {
        continue;
      }

      LocalDateTime appointmentStart = appointment.getRequestedDateTime();
      LocalDateTime appointmentEnd = appointmentStart.plusMinutes(60); // Assume 60 min default duration

      // Check for overlap
      if (slotStart.isBefore(appointmentEnd) && slotEnd.isAfter(appointmentStart)) {
        return false;
      }
    }
    return true;
  }

  private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
    // Define valid transitions
    List<AppointmentStatus> validTransitions;

    switch (currentStatus) {
      case PENDING:
        validTransitions = Arrays.asList(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED);
        break;
      case CONFIRMED:
        validTransitions = Arrays.asList(AppointmentStatus.IN_PROGRESS, AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW);
        break;
      case IN_PROGRESS:
        validTransitions = Arrays.asList(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED);
        break;
      case COMPLETED:
      case CANCELLED:
      case NO_SHOW:
        validTransitions = List.of(); // Terminal states
        break;
      default:
        validTransitions = List.of();
    }

    if (!validTransitions.contains(newStatus)) {
      throw new InvalidStatusTransitionException(currentStatus, newStatus);
    }
  }
}