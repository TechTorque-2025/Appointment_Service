package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.dto.request.*;
import com.techtorque.appointment_service.dto.response.*;
import com.techtorque.appointment_service.entity.*;
import com.techtorque.appointment_service.exception.*;
import com.techtorque.appointment_service.repository.*;
import com.techtorque.appointment_service.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for AppointmentServiceImpl
 * Tests all business logic, validation, and error handling scenarios
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ServiceTypeService serviceTypeService;

    @Mock
    private ServiceBayRepository serviceBayRepository;

    @Mock
    private BusinessHoursRepository businessHoursRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private TimeSessionRepository timeSessionRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private com.techtorque.appointment_service.client.TimeLoggingClient timeLoggingClient;

    @Mock
    private AppointmentStateTransitionValidator stateTransitionValidator;

    private AppointmentServiceImpl appointmentService;

    private ServiceBay testBay;
    private BusinessHours testBusinessHours;
    private Appointment testAppointment;
    private ServiceTypeResponseDto testServiceType;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(
                appointmentRepository,
                serviceTypeService,
                serviceBayRepository,
                businessHoursRepository,
                holidayRepository,
                timeSessionRepository,
                notificationClient,
                timeLoggingClient,
                stateTransitionValidator);

        setupTestData();
    }

    private void setupTestData() {
        // Test service bay
        testBay = ServiceBay.builder()
                .id("bay-1")
                .name("Bay 1")
                .bayNumber("BAY-01")
                .active(true)
                .build(); // Test business hours - Monday 9 AM to 5 PM
        testBusinessHours = BusinessHours.builder()
                .id("bh-1")
                .dayOfWeek(DayOfWeek.SUNDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(17, 0))
                .isOpen(true)
                .build();

        // Test appointment - use future date
        testAppointment = Appointment.builder()
                .id("apt-1")
                .customerId("customer-1")
                .vehicleId("vehicle-1")
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.of(2025, 6, 15, 10, 0)) // Future date
                .status(AppointmentStatus.PENDING)
                .confirmationNumber("APT-2025-001000")
                .assignedBayId("bay-1")
                .assignedEmployeeIds(new HashSet<>())
                .build(); // Test service type
        testServiceType = ServiceTypeResponseDto.builder()
                .id("st-1")
                .name("Oil Change")
                .estimatedDurationMinutes(60)
                .active(true)
                .build();
    }

    @Test
    void bookAppointment_Success() {
        // Given
        AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                .vehicleId("vehicle-1")
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.of(2025, 6, 15, 10, 0)) // Future date
                .specialInstructions("Please check tire pressure")
                .build();
        when(serviceTypeService.getAllServiceTypes(false))
                .thenReturn(List.of(testServiceType));
        when(holidayRepository.existsByDate(any(LocalDate.class))).thenReturn(false);
        when(businessHoursRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(testBusinessHours));
        when(serviceBayRepository.findByActiveTrueOrderByBayNumberAsc())
                .thenReturn(List.of(testBay));
        when(appointmentRepository.findByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot(
                any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findMaxConfirmationNumberByPrefix(anyString()))
                .thenReturn(Optional.of("APT-2025-000999"));
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(testAppointment);

        // When
        AppointmentResponseDto result = appointmentService.bookAppointment(requestDto, "customer-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo("customer-1");
        assertThat(result.getServiceType()).isEqualTo("Oil Change");
        assertThat(result.getStatus()).isEqualTo(AppointmentStatus.PENDING);

        verify(appointmentRepository).save(any(Appointment.class));
        verify(notificationClient).sendAppointmentNotification(eq("customer-1"), eq("INFO"), any(), any(), any());
    }

    @Test
    void bookAppointment_InvalidServiceType_ThrowsException() {
        // Given
        AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                .serviceType("Invalid Service")
                .build();

        when(serviceTypeService.getAllServiceTypes(false)).thenReturn(List.of(testServiceType));

        // When & Then
        assertThatThrownBy(() -> appointmentService.bookAppointment(requestDto, "customer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid service type");
    }

    @Test
    void bookAppointment_PastDateTime_ThrowsException() {
        // Given
        AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.now().minusDays(1))
                .build();

        when(serviceTypeService.getAllServiceTypes(false)).thenReturn(List.of(testServiceType));

        // When & Then
        assertThatThrownBy(() -> appointmentService.bookAppointment(requestDto, "customer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment date must be in the future");
    }

    @Test
    void bookAppointment_Holiday_ThrowsException() {
        // Given
        AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.of(2025, 12, 25, 10, 0)) // Future Christmas
                .build();

        when(serviceTypeService.getAllServiceTypes(false)).thenReturn(List.of(testServiceType));
        when(holidayRepository.existsByDate(LocalDate.of(2025, 12, 25))).thenReturn(true); // When & Then
        assertThatThrownBy(() -> appointmentService.bookAppointment(requestDto, "customer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot book appointment on a holiday");
    }

    @Test
    void bookAppointment_OutsideBusinessHours_ThrowsException() {
        // Given
        AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.of(2024, 1, 15, 8, 0)) // Before opening
                .build();

        when(serviceTypeService.getAllServiceTypes(false)).thenReturn(List.of(testServiceType));
        when(holidayRepository.existsByDate(any())).thenReturn(false);
        when(businessHoursRepository.findByDayOfWeek(DayOfWeek.SUNDAY))
                .thenReturn(Optional.of(testBusinessHours)); // When & Then
        assertThatThrownBy(() -> appointmentService.bookAppointment(requestDto, "customer-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Requested time is outside business hours");
    }

    @Test
    void bookAppointment_NoBaysAvailable_ThrowsException() {
        // Given
        AppointmentRequestDto requestDto = AppointmentRequestDto.builder()
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.of(2024, 1, 15, 10, 0))
                .build();

        when(serviceTypeService.getAllServiceTypes(false)).thenReturn(List.of(testServiceType));
        when(holidayRepository.existsByDate(any())).thenReturn(false);
        when(businessHoursRepository.findByDayOfWeek(DayOfWeek.MONDAY))
                .thenReturn(Optional.of(testBusinessHours));
        when(serviceBayRepository.findByActiveTrueOrderByBayNumberAsc()).thenReturn(List.of());

        // When & Then
        assertThatThrownBy(() -> appointmentService.bookAppointment(requestDto, "customer-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No service bays available");
    }

    @Test
    void getAppointmentsForUser_Customer_Success() {
        // Given
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findByCustomerIdOrderByRequestedDateTimeDesc("customer-1"))
                .thenReturn(appointments);

        // When
        List<AppointmentResponseDto> result = appointmentService
                .getAppointmentsForUser("customer-1", "CUSTOMER");

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo("customer-1");
    }

    @Test
    void getAppointmentsForUser_Employee_Success() {
        // Given
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findByAssignedEmployeeIdAndRequestedDateTimeBetween(
                eq("employee-1"), any(), any())).thenReturn(appointments);

        // When
        List<AppointmentResponseDto> result = appointmentService
                .getAppointmentsForUser("employee-1", "EMPLOYEE");

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getAppointmentsForUser_Admin_Success() {
        // Given
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findAll()).thenReturn(appointments);

        // When
        List<AppointmentResponseDto> result = appointmentService
                .getAppointmentsForUser("admin-1", "ADMIN");

        // Then
        assertThat(result).hasSize(1);
    }

    @Test
    void getAppointmentDetails_Customer_Success() {
        // Given
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));

        // When
        AppointmentResponseDto result = appointmentService
                .getAppointmentDetails("apt-1", "customer-1", "CUSTOMER");

        // Then
        assertThat(result.getId()).isEqualTo("apt-1");
        assertThat(result.getCustomerId()).isEqualTo("customer-1");
    }

    @Test
    void getAppointmentDetails_UnauthorizedCustomer_ThrowsException() {
        // Given
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService
                .getAppointmentDetails("apt-1", "other-customer", "CUSTOMER"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You do not have permission to view this appointment");
    }

    @Test
    void updateAppointment_Success() {
        // Given
        AppointmentUpdateDto updateDto = AppointmentUpdateDto.builder()
                .requestedDateTime(LocalDateTime.of(2025, 6, 16, 11, 0)) // Future date
                .specialInstructions("Updated instructions")
                .build();
        when(appointmentRepository.findByIdAndCustomerId("apt-1", "customer-1"))
                .thenReturn(Optional.of(testAppointment));
        when(holidayRepository.existsByDate(any())).thenReturn(false);
        when(businessHoursRepository.findByDayOfWeek(any()))
                .thenReturn(Optional.of(testBusinessHours));
        when(serviceBayRepository.findByActiveTrueOrderByBayNumberAsc())
                .thenReturn(List.of(testBay));
        when(appointmentRepository.findByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot(
                any(), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        AppointmentResponseDto result = appointmentService
                .updateAppointment("apt-1", updateDto, "customer-1");

        // Then
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(any());
        verify(notificationClient).sendAppointmentNotification(
                eq("customer-1"), eq("INFO"), any(), any(), eq("apt-1"));
    }

    @Test
    void updateAppointment_InvalidStatus_ThrowsException() {
        // Given
        testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
        AppointmentUpdateDto updateDto = AppointmentUpdateDto.builder()
                .requestedDateTime(LocalDateTime.of(2024, 1, 16, 11, 0))
                .build();

        when(appointmentRepository.findByIdAndCustomerId("apt-1", "customer-1"))
                .thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService
                .updateAppointment("apt-1", updateDto, "customer-1"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Cannot update appointment with status");
    }

    @Test
    void cancelAppointment_Customer_Success() {
        // Given
        when(appointmentRepository.findByIdAndCustomerId("apt-1", "customer-1"))
                .thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        appointmentService.cancelAppointment("apt-1", "customer-1", "CUSTOMER");

        // Then
        verify(appointmentRepository).save(argThat(apt -> apt.getStatus() == AppointmentStatus.CANCELLED));
        verify(notificationClient).sendAppointmentNotification(
                eq("customer-1"), eq("WARNING"), eq("Appointment Cancelled"), any(), eq("apt-1"));
    }

    @Test
    void cancelAppointment_InProgressByCustomer_ThrowsException() {
        // Given
        testAppointment.setStatus(AppointmentStatus.IN_PROGRESS);
        when(appointmentRepository.findByIdAndCustomerId("apt-1", "customer-1"))
                .thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService
                .cancelAppointment("apt-1", "customer-1", "CUSTOMER"))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("Cannot cancel an appointment that is currently in progress");
    }

    @Test
    void updateAppointmentStatus_Success() {
        // Given
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        AppointmentResponseDto result = appointmentService
                .updateAppointmentStatus("apt-1", AppointmentStatus.CONFIRMED, "employee-1");

        // Then
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(argThat(apt -> apt.getStatus() == AppointmentStatus.CONFIRMED));
        verify(notificationClient).sendAppointmentNotification(
                eq("customer-1"), eq("SUCCESS"), any(), any(), eq("apt-1"));
    }

    @Test
    void checkAvailability_Holiday_ReturnsEmptySlots() {
        // Given
        LocalDate holidayDate = LocalDate.of(2025, 12, 25); // Future Christmas
        when(holidayRepository.existsByDate(holidayDate)).thenReturn(true);

        // When
        AvailabilityResponseDto result = appointmentService
                .checkAvailability(holidayDate, "Oil Change", 60);

        // Then
        assertThat(result.getAvailableSlots()).isEmpty();
    }

    @Test
    void checkAvailability_ClosedDay_ReturnsEmptySlots() {
        // Given
        LocalDate date = LocalDate.of(2025, 1, 19); // Future Sunday
        BusinessHours closedHours = BusinessHours.builder()
                .dayOfWeek(DayOfWeek.SUNDAY)
                .isOpen(false)
                .build();

        when(holidayRepository.existsByDate(date)).thenReturn(false);
        when(businessHoursRepository.findByDayOfWeek(DayOfWeek.SUNDAY))
                .thenReturn(Optional.of(closedHours));

        // When
        AvailabilityResponseDto result = appointmentService
                .checkAvailability(date, "Oil Change", 60);

        // Then
        assertThat(result.getAvailableSlots()).isEmpty();
    }

    @Test
    void assignEmployees_Success() {
        // Given
        Set<String> employeeIds = Set.of("emp-1", "emp-2");
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        AppointmentResponseDto result = appointmentService
                .assignEmployees("apt-1", employeeIds, "admin-1");

        // Then
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(argThat(apt -> apt.getAssignedEmployeeIds().containsAll(employeeIds) &&
                apt.getStatus() == AppointmentStatus.CONFIRMED));
        verify(notificationClient, times(3)).sendAppointmentNotification(any(), any(), any(), any(), any());
    }

    @Test
    void assignEmployees_CompletedAppointment_ThrowsException() {
        // Given
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService
                .assignEmployees("apt-1", Set.of("emp-1"), "admin-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot assign employees to a COMPLETED appointment");
    }

    @Test
    void acceptVehicleArrival_Success() {
        // Given
        testAppointment.setStatus(AppointmentStatus.CONFIRMED);
        testAppointment.getAssignedEmployeeIds().add("emp-1");

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(testAppointment);
        when(timeLoggingClient.createTimeLog(any(), any(), any(), anyDouble())).thenReturn("time-log-1");
        when(timeSessionRepository.save(any())).thenReturn(new TimeSession());

        // When
        AppointmentResponseDto result = appointmentService
                .acceptVehicleArrival("apt-1", "emp-1");

        // Then
        assertThat(result).isNotNull();
        verify(appointmentRepository, times(2)).save(any()); // Called twice: once in acceptVehicleArrival, once in
                                                             // clockIn
        verify(timeSessionRepository).save(any());
        verify(notificationClient).sendAppointmentNotification(
                eq("customer-1"), eq("INFO"), eq("Work Started"), any(), eq("apt-1"));
    }

    @Test
    void acceptVehicleArrival_UnauthorizedEmployee_ThrowsException() {
        // Given
        testAppointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService
                .acceptVehicleArrival("apt-1", "unauthorized-emp"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("Employee is not assigned to this appointment");
    }

    @Test
    void clockIn_Success() {
        // Given
        testAppointment.getAssignedEmployeeIds().add("emp-1");
        TimeSession timeSession = new TimeSession();
        timeSession.setId("session-1");
        timeSession.setAppointmentId("apt-1");
        timeSession.setEmployeeId("emp-1");
        timeSession.setActive(true);
        timeSession.setClockInTime(LocalDateTime.now());

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(timeSessionRepository.findByAppointmentIdAndEmployeeIdAndActiveTrue("apt-1", "emp-1"))
                .thenReturn(Optional.empty());
        when(timeLoggingClient.createTimeLog(any(), any(), any(), anyDouble())).thenReturn("time-log-1");
        when(timeSessionRepository.save(any())).thenReturn(timeSession);
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        TimeSessionResponse result = appointmentService.clockIn("apt-1", "emp-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAppointmentId()).isEqualTo("apt-1");
        assertThat(result.isActive()).isTrue();
        verify(timeSessionRepository).save(any());
        verify(appointmentRepository).save(argThat(apt -> apt.getStatus() == AppointmentStatus.IN_PROGRESS));
    }

    @Test
    void clockIn_AlreadyActive_ReturnsExistingSession() {
        // Given
        testAppointment.getAssignedEmployeeIds().add("emp-1");
        TimeSession existingSession = new TimeSession();
        existingSession.setId("session-1");
        existingSession.setAppointmentId("apt-1");
        existingSession.setEmployeeId("emp-1");
        existingSession.setActive(true);
        existingSession.setClockInTime(LocalDateTime.now());

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(timeSessionRepository.findByAppointmentIdAndEmployeeIdAndActiveTrue("apt-1", "emp-1"))
                .thenReturn(Optional.of(existingSession));

        // When
        TimeSessionResponse result = appointmentService.clockIn("apt-1", "emp-1");

        // Then
        assertThat(result).isNotNull();
        verify(timeSessionRepository, never()).save(any());
        verify(timeLoggingClient, never()).createTimeLog(any(), any(), any(), anyDouble());
    }

    @Test
    void clockOut_Success() {
        // Given
        TimeSession activeSession = new TimeSession();
        activeSession.setId("session-1");
        activeSession.setAppointmentId("apt-1");
        activeSession.setEmployeeId("emp-1");
        activeSession.setActive(true);
        activeSession.setClockInTime(LocalDateTime.now().minusHours(2));
        activeSession.setTimeLogId("time-log-1");

        when(timeSessionRepository.findByAppointmentIdAndEmployeeIdAndActiveTrue("apt-1", "emp-1"))
                .thenReturn(Optional.of(activeSession));
        when(timeSessionRepository.save(any())).thenReturn(activeSession);
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        TimeSessionResponse result = appointmentService.clockOut("apt-1", "emp-1");

        // Then
        assertThat(result).isNotNull();
        verify(timeSessionRepository).save(argThat(session -> !session.isActive()));
        verify(timeLoggingClient).updateTimeLog(eq("time-log-1"), anyDouble(), any());
        verify(appointmentRepository).save(argThat(apt -> apt.getStatus() == AppointmentStatus.COMPLETED));
    }

    @Test
    void clockOut_NoActiveSession_ThrowsException() {
        // Given
        when(timeSessionRepository.findByAppointmentIdAndEmployeeIdAndActiveTrue("apt-1", "emp-1"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> appointmentService.clockOut("apt-1", "emp-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No active time session found");
    }

    @Test
    void getActiveTimeSession_Found() {
        // Given
        TimeSession activeSession = new TimeSession();
        activeSession.setId("session-1");
        activeSession.setActive(true);
        activeSession.setClockInTime(LocalDateTime.now().minusMinutes(30));

        when(timeSessionRepository.findByAppointmentIdAndEmployeeIdAndActiveTrue("apt-1", "emp-1"))
                .thenReturn(Optional.of(activeSession));

        // When
        TimeSessionResponse result = appointmentService.getActiveTimeSession("apt-1", "emp-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isActive()).isTrue();
        assertThat(result.getElapsedSeconds()).isPositive();
    }

    @Test
    void getActiveTimeSession_NotFound() {
        // Given
        when(timeSessionRepository.findByAppointmentIdAndEmployeeIdAndActiveTrue("apt-1", "emp-1"))
                .thenReturn(Optional.empty());

        // When
        TimeSessionResponse result = appointmentService.getActiveTimeSession("apt-1", "emp-1");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void confirmCompletion_Success() {
        // Given
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        testAppointment.getAssignedEmployeeIds().addAll(Set.of("emp-1", "emp-2"));

        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));
        when(appointmentRepository.save(any())).thenReturn(testAppointment);

        // When
        AppointmentResponseDto result = appointmentService.confirmCompletion("apt-1", "customer-1");

        // Then
        assertThat(result).isNotNull();
        verify(appointmentRepository).save(argThat(apt -> apt.getStatus() == AppointmentStatus.CUSTOMER_CONFIRMED));
        verify(notificationClient, times(3)).sendAppointmentNotification(any(), any(), any(), any(), any());
    }

    @Test
    void confirmCompletion_WrongCustomer_ThrowsException() {
        // Given
        testAppointment.setStatus(AppointmentStatus.COMPLETED);
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService.confirmCompletion("apt-1", "wrong-customer"))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You do not have permission to confirm this appointment");
    }

    @Test
    void confirmCompletion_WrongStatus_ThrowsException() {
        // Given
        testAppointment.setStatus(AppointmentStatus.PENDING);
        when(appointmentRepository.findById("apt-1")).thenReturn(Optional.of(testAppointment));

        // When & Then
        assertThatThrownBy(() -> appointmentService.confirmCompletion("apt-1", "customer-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only confirm completion for COMPLETED appointments");
    }

    @Test
    void getAppointmentsWithFilters_Success() {
        // Given
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(appointments);

        // When
        List<AppointmentResponseDto> result = appointmentService.getAppointmentsWithFilters(
                "customer-1", "vehicle-1", AppointmentStatus.PENDING,
                LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 30)); // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerId()).isEqualTo("customer-1");
    }

    @Test
    void getEmployeeSchedule_Success() {
        // Given
        List<Appointment> appointments = List.of(testAppointment);
        when(appointmentRepository.findByAssignedEmployeeIdAndRequestedDateTimeBetween(
                eq("emp-1"), any(), any())).thenReturn(appointments);

        // When
        ScheduleResponseDto result = appointmentService
                .getEmployeeSchedule("emp-1", LocalDate.of(2025, 6, 15)); // Then
        assertThat(result.getEmployeeId()).isEqualTo("emp-1");
        assertThat(result.getAppointments()).hasSize(1);
    }

    @Test
    void getMonthlyCalendar_Success() {
        // Given
        List<Appointment> appointments = List.of(testAppointment);
        List<Holiday> holidays = List.of();
        List<ServiceBay> bays = List.of(testBay);

        when(appointmentRepository.findByRequestedDateTimeBetween(any(), any()))
                .thenReturn(appointments);
        when(holidayRepository.findAll()).thenReturn(holidays);
        when(serviceBayRepository.findAll()).thenReturn(bays);

        // When
        CalendarResponseDto result = appointmentService
                .getMonthlyCalendar(YearMonth.of(2025, 6), "ADMIN");

        // Then
        assertThat(result.getMonth()).isEqualTo(YearMonth.of(2025, 6));
        assertThat(result.getDays()).hasSize(30); // June has 30 days
        assertThat(result.getStatistics().getTotalAppointments()).isEqualTo(1);
    }
}