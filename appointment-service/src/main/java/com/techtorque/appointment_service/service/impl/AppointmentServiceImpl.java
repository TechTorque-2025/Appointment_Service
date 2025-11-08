package com.techtorque.appointment_service.service.impl;

import com.techtorque.appointment_service.dto.request.*;
import com.techtorque.appointment_service.dto.response.*;
import com.techtorque.appointment_service.entity.*;
import com.techtorque.appointment_service.exception.*;
import com.techtorque.appointment_service.repository.*;
import com.techtorque.appointment_service.service.AppointmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final ServiceTypeRepository serviceTypeRepository;
  private final ServiceBayRepository serviceBayRepository;
  private final BusinessHoursRepository businessHoursRepository;
  private final HolidayRepository holidayRepository;

  private static final int SLOT_INTERVAL_MINUTES = 30;

  public AppointmentServiceImpl(
      AppointmentRepository appointmentRepository,
      ServiceTypeRepository serviceTypeRepository,
      ServiceBayRepository serviceBayRepository,
      BusinessHoursRepository businessHoursRepository,
      HolidayRepository holidayRepository) {
    this.appointmentRepository = appointmentRepository;
    this.serviceTypeRepository = serviceTypeRepository;
    this.serviceBayRepository = serviceBayRepository;
    this.businessHoursRepository = businessHoursRepository;
    this.holidayRepository = holidayRepository;
  }

  @Override
  public AppointmentResponseDto bookAppointment(AppointmentRequestDto dto, String customerId) {
    log.info("Booking appointment for customer: {}", customerId);

    ServiceType serviceType = serviceTypeRepository.findByNameAndActiveTrue(dto.getServiceType())
        .orElseThrow(() -> new IllegalArgumentException("Invalid service type: " + dto.getServiceType()));

    validateAppointmentDateTime(dto.getRequestedDateTime(), serviceType.getEstimatedDurationMinutes());

    String confirmationNumber = generateConfirmationNumber();
    String assignedBayId = findAvailableBay(dto.getRequestedDateTime(), serviceType.getEstimatedDurationMinutes());

    Appointment appointment = Appointment.builder()
        .customerId(customerId)
        .vehicleId(dto.getVehicleId())
        .serviceType(dto.getServiceType())
        .requestedDateTime(dto.getRequestedDateTime())
        .specialInstructions(dto.getSpecialInstructions())
        .status(AppointmentStatus.PENDING)
        .confirmationNumber(confirmationNumber)
        .assignedBayId(assignedBayId)
        .build();

    Appointment savedAppointment = appointmentRepository.save(appointment);
    log.info("Appointment booked successfully with confirmation: {}", confirmationNumber);

    return convertToDto(savedAppointment);
  }

  @Override
  public List<AppointmentResponseDto> getAppointmentsForUser(String userId, String userRoles) {
    log.info("Fetching appointments for user: {} with roles: {}", userId, userRoles);

    List<Appointment> appointments;

    if (userRoles.contains("ADMIN") || userRoles.contains("EMPLOYEE")) {
      appointments = appointmentRepository.findAll();
    } else if (userRoles.contains("EMPLOYEE")) {
      appointments = appointmentRepository.findByAssignedEmployeeIdAndRequestedDateTimeBetween(
          userId, LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1));
    } else {
      appointments = appointmentRepository.findByCustomerIdOrderByRequestedDateTimeDesc(userId);
    }

    log.info("Found {} appointments for user", appointments != null ? appointments.size() : 0);

    if (appointments == null) {
      return List.of();
    }

    return appointments.stream()
        .filter(appointment -> appointment != null)
        .map(this::convertToDto)
        .collect(Collectors.toList());
  }

  @Override
  public List<AppointmentResponseDto> getAppointmentsWithFilters(
      String customerId, String vehicleId, AppointmentStatus status, 
      LocalDate fromDate, LocalDate toDate) {
    log.info("Fetching appointments with filters - customerId: {}, vehicleId: {}, status: {}, fromDate: {}, toDate: {}", 
        customerId, vehicleId, status, fromDate, toDate);

    LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
    LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

    // Convert AppointmentStatus enum to String for the native query
    String statusString = status != null ? status.name() : null;

    List<Appointment> appointments = appointmentRepository.findWithFilters(
        customerId, vehicleId, statusString, fromDateTime, toDateTime);

    log.info("Found {} appointments matching filters", appointments != null ? appointments.size() : 0);

    if (appointments == null) {
      return List.of();
    }

    return appointments.stream()
        .filter(appointment -> appointment != null)
        .map(this::convertToDto)
        .collect(Collectors.toList());
  }

  @Override
  public AppointmentResponseDto getAppointmentDetails(String appointmentId, String userId, String userRoles) {
    log.info("Fetching appointment details for ID: {} by user: {}", appointmentId, userId);

    Appointment appointment = appointmentRepository.findById(appointmentId)
        .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

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

    if (appointment.getStatus() != AppointmentStatus.PENDING &&
        appointment.getStatus() != AppointmentStatus.CONFIRMED) {
      throw new InvalidStatusTransitionException("Cannot update appointment with status: " + appointment.getStatus());
    }

    if (dto.getRequestedDateTime() != null) {
      validateAppointmentDateTime(dto.getRequestedDateTime(), 60);
      appointment.setRequestedDateTime(dto.getRequestedDateTime());
      String newBayId = findAvailableBay(dto.getRequestedDateTime(), 60);
      appointment.setAssignedBayId(newBayId);
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

    validateStatusTransition(appointment.getStatus(), newStatus);

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

    if (holidayRepository.existsByDate(date)) {
      return AvailabilityResponseDto.builder()
          .date(date).serviceType(serviceType).durationMinutes(duration)
          .availableSlots(Collections.emptyList()).build();
    }

    BusinessHours businessHours = businessHoursRepository.findByDayOfWeek(date.getDayOfWeek()).orElse(null);

    if (businessHours == null || !businessHours.getIsOpen()) {
      return AvailabilityResponseDto.builder()
          .date(date).serviceType(serviceType).durationMinutes(duration)
          .availableSlots(Collections.emptyList()).build();
    }

    List<ServiceBay> bays = serviceBayRepository.findByActiveTrue();
    List<TimeSlotDto> slots = generateTimeSlots(date, duration, businessHours, bays);

    return AvailabilityResponseDto.builder()
        .date(date).serviceType(serviceType).durationMinutes(duration)
        .availableSlots(slots).build();
  }

  @Override
  public ScheduleResponseDto getEmployeeSchedule(String employeeId, LocalDate date) {
    log.info("Fetching schedule for employee: {} on date: {}", employeeId, date);

    LocalDateTime dayStart = date.atStartOfDay();
    LocalDateTime dayEnd = date.atTime(23, 59, 59);

    List<Appointment> appointments = appointmentRepository
        .findByAssignedEmployeeIdAndRequestedDateTimeBetween(employeeId, dayStart, dayEnd);

    List<ScheduleItemDto> scheduleItems = appointments.stream()
        .map(this::convertToScheduleItem).collect(Collectors.toList());

    return ScheduleResponseDto.builder()
        .employeeId(employeeId).date(date).appointments(scheduleItems).build();
  }

  @Override
  public CalendarResponseDto getMonthlyCalendar(YearMonth month, String userRole) {
    log.info("Fetching calendar for month: {}", month);

    LocalDate startDate = month.atDay(1);
    LocalDate endDate = month.atEndOfMonth();
    
    LocalDateTime startDateTime = startDate.atStartOfDay();
    LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

    List<Appointment> appointments = appointmentRepository.findByRequestedDateTimeBetween(startDateTime, endDateTime);

    Map<LocalDate, Holiday> holidays = holidayRepository.findAll().stream()
        .filter(h -> !h.getDate().isBefore(startDate) && !h.getDate().isAfter(endDate))
        .collect(Collectors.toMap(Holiday::getDate, h -> h));

    Map<String, String> bayIdToName = serviceBayRepository.findAll().stream()
        .collect(Collectors.toMap(ServiceBay::getId, ServiceBay::getName));

    Map<LocalDate, List<Appointment>> appointmentsByDate = appointments.stream()
        .collect(Collectors.groupingBy(a -> a.getRequestedDateTime().toLocalDate()));

    List<CalendarDayDto> days = new ArrayList<>();
    for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
      List<Appointment> dayAppointments = appointmentsByDate.getOrDefault(date, Collections.emptyList());
      Holiday holiday = holidays.get(date);

      List<AppointmentSummaryDto> summaries = dayAppointments.stream()
          .map(a -> convertToSummary(a, bayIdToName)).collect(Collectors.toList());

      days.add(CalendarDayDto.builder()
          .date(date).appointmentCount(dayAppointments.size())
          .isHoliday(holiday != null).holidayName(holiday != null ? holiday.getName() : null)
          .appointments(summaries).build());
    }

    CalendarStatisticsDto statistics = calculateStatistics(appointments, bayIdToName);

    return CalendarResponseDto.builder()
        .month(month).days(days).statistics(statistics).build();
  }

  // Helper methods

  private void validateAppointmentDateTime(LocalDateTime dateTime, int durationMinutes) {
    LocalDate date = dateTime.toLocalDate();
    LocalTime time = dateTime.toLocalTime();

    if (dateTime.isBefore(LocalDateTime.now())) {
      throw new IllegalArgumentException("Appointment date must be in the future");
    }

    if (holidayRepository.existsByDate(date)) {
      throw new IllegalArgumentException("Cannot book appointment on a holiday");
    }

    BusinessHours businessHours = businessHoursRepository.findByDayOfWeek(date.getDayOfWeek())
        .orElseThrow(() -> new IllegalArgumentException("No business hours configured for this day"));

    if (!businessHours.getIsOpen()) {
      throw new IllegalArgumentException("Shop is closed on " + date.getDayOfWeek());
    }

    if (time.isBefore(businessHours.getOpenTime()) || 
        time.plusMinutes(durationMinutes).isAfter(businessHours.getCloseTime())) {
      throw new IllegalArgumentException("Requested time is outside business hours");
    }

    if (businessHours.getBreakStartTime() != null && businessHours.getBreakEndTime() != null) {
      if (!time.isBefore(businessHours.getBreakStartTime()) && 
          time.isBefore(businessHours.getBreakEndTime())) {
        throw new IllegalArgumentException("Cannot book appointment during break time");
      }
    }
  }

  private String findAvailableBay(LocalDateTime requestedDateTime, int durationMinutes) {
    List<ServiceBay> bays = serviceBayRepository.findByActiveTrueOrderByBayNumberAsc();
    
    if (bays.isEmpty()) {
      throw new IllegalStateException("No service bays available");
    }

    LocalDateTime slotEnd = requestedDateTime.plusMinutes(durationMinutes);

    for (ServiceBay bay : bays) {
      List<Appointment> bayAppointments = appointmentRepository
          .findByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot(
              bay.getId(), requestedDateTime.minusHours(2), requestedDateTime.plusHours(2),
              AppointmentStatus.CANCELLED);

      boolean isAvailable = bayAppointments.stream()
          .noneMatch(apt -> {
            LocalDateTime aptStart = apt.getRequestedDateTime();
            LocalDateTime aptEnd = aptStart.plusMinutes(60);
            return slotEnd.isAfter(aptStart) && requestedDateTime.isBefore(aptEnd);
          });

      if (isAvailable) {
        return bay.getId();
      }
    }

    throw new IllegalArgumentException("No bays available at the requested time");
  }

  private String generateConfirmationNumber() {
    LocalDate today = LocalDate.now();
    String prefix = "APT-" + today.getYear() + "-";
    
    Optional<String> maxConfirmationOpt = appointmentRepository.findMaxConfirmationNumberByPrefix(prefix);
    
    int nextNumber = 1000;
    if (maxConfirmationOpt.isPresent() && maxConfirmationOpt.get() != null) {
      String maxConfirmation = maxConfirmationOpt.get();
      String numberPart = maxConfirmation.substring(prefix.length());
      nextNumber = Integer.parseInt(numberPart) + 1;
    }
    
    return prefix + String.format("%06d", nextNumber);
  }

  private List<TimeSlotDto> generateTimeSlots(LocalDate date, int durationMinutes, 
                                               BusinessHours businessHours, List<ServiceBay> bays) {
    List<TimeSlotDto> slots = new ArrayList<>();
    LocalDateTime currentSlot = date.atTime(businessHours.getOpenTime());
    LocalDateTime endOfDay = date.atTime(businessHours.getCloseTime());

    while (currentSlot.plusMinutes(durationMinutes).isBefore(endOfDay) ||
           currentSlot.plusMinutes(durationMinutes).equals(endOfDay)) {

      if (businessHours.getBreakStartTime() != null && businessHours.getBreakEndTime() != null) {
        LocalTime slotTime = currentSlot.toLocalTime();
        if (!slotTime.isBefore(businessHours.getBreakStartTime()) && 
            slotTime.isBefore(businessHours.getBreakEndTime())) {
          currentSlot = currentSlot.plusMinutes(SLOT_INTERVAL_MINUTES);
          continue;
        }
      }

      LocalDateTime slotEnd = currentSlot.plusMinutes(durationMinutes);

      for (ServiceBay bay : bays) {
        boolean isAvailable = isBayAvailable(bay.getId(), currentSlot, slotEnd);

        if (isAvailable) {
          slots.add(TimeSlotDto.builder()
              .startTime(currentSlot).endTime(slotEnd).available(true)
              .bayId(bay.getId()).bayName(bay.getName()).build());
          break;
        }
      }

      currentSlot = currentSlot.plusMinutes(SLOT_INTERVAL_MINUTES);
    }

    return slots;
  }

  private boolean isBayAvailable(String bayId, LocalDateTime slotStart, LocalDateTime slotEnd) {
    List<Appointment> bayAppointments = appointmentRepository
        .findByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot(
            bayId, slotStart.minusHours(1), slotEnd.plusHours(1), AppointmentStatus.CANCELLED);

    for (Appointment appointment : bayAppointments) {
      if (appointment.getStatus() == AppointmentStatus.NO_SHOW) {
        continue;
      }

      LocalDateTime aptStart = appointment.getRequestedDateTime();
      LocalDateTime aptEnd = aptStart.plusMinutes(60);

      if (slotStart.isBefore(aptEnd) && slotEnd.isAfter(aptStart)) {
        return false;
      }
    }
    return true;
  }

  private void validateStatusTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
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
        validTransitions = List.of();
        break;
      default:
        validTransitions = List.of();
    }

    if (!validTransitions.contains(newStatus)) {
      throw new InvalidStatusTransitionException(currentStatus, newStatus);
    }
  }

  private CalendarStatisticsDto calculateStatistics(List<Appointment> appointments, Map<String, String> bayIdToName) {
    Map<String, Integer> byServiceType = new HashMap<>();
    Map<String, Integer> byBay = new HashMap<>();
    
    int completed = 0, pending = 0, confirmed = 0, cancelled = 0;

    for (Appointment apt : appointments) {
      switch (apt.getStatus()) {
        case COMPLETED:
          completed++;
          break;
        case PENDING:
          pending++;
          break;
        case CONFIRMED:
        case IN_PROGRESS:
          confirmed++;
          break;
        case CANCELLED:
        case NO_SHOW:
          cancelled++;
          break;
      }

      byServiceType.merge(apt.getServiceType(), 1, Integer::sum);

      if (apt.getAssignedBayId() != null) {
        String bayName = bayIdToName.getOrDefault(apt.getAssignedBayId(), "Unknown Bay");
        byBay.merge(bayName, 1, Integer::sum);
      }
    }

    return CalendarStatisticsDto.builder()
        .totalAppointments(appointments.size())
        .completedAppointments(completed)
        .pendingAppointments(pending)
        .confirmedAppointments(confirmed)
        .cancelledAppointments(cancelled)
        .appointmentsByServiceType(byServiceType)
        .appointmentsByBay(byBay)
        .build();
  }

  private AppointmentResponseDto convertToDto(Appointment appointment) {
    return AppointmentResponseDto.builder()
        .id(appointment.getId())
        .customerId(appointment.getCustomerId())
        .vehicleId(appointment.getVehicleId())
        .assignedEmployeeId(appointment.getAssignedEmployeeId())
        .assignedBayId(appointment.getAssignedBayId())
        .confirmationNumber(appointment.getConfirmationNumber())
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

  private AppointmentSummaryDto convertToSummary(Appointment appointment, Map<String, String> bayIdToName) {
    String bayName = appointment.getAssignedBayId() != null 
        ? bayIdToName.getOrDefault(appointment.getAssignedBayId(), "Not Assigned")
        : "Not Assigned";

    return AppointmentSummaryDto.builder()
        .id(appointment.getId())
        .confirmationNumber(appointment.getConfirmationNumber())
        .time(appointment.getRequestedDateTime())
        .serviceType(appointment.getServiceType())
        .status(appointment.getStatus())
        .bayName(bayName)
        .build();
  }
}
