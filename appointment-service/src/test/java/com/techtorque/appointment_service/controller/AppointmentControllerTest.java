package com.techtorque.appointment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techtorque.appointment_service.config.SecurityConfig;
import com.techtorque.appointment_service.dto.request.*;
import com.techtorque.appointment_service.dto.response.*;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import com.techtorque.appointment_service.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AppointmentController
 * Tests all REST endpoints with proper security, validation, and error handling
 */
@WebMvcTest(AppointmentController.class)
@ActiveProfiles("test")
@Import(SecurityConfig.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppointmentService appointmentService;

    // Test data
    private AppointmentResponseDto createTestAppointmentResponse() {
        return AppointmentResponseDto.builder()
                .id("apt-1")
                .customerId("customer-1")
                .vehicleId("vehicle-1")
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.of(2025, 6, 15, 10, 0))
                .status(AppointmentStatus.PENDING)
                .confirmationNumber("APT-2025-001000")
                .assignedBayId("bay-1")
                .assignedEmployeeIds(Set.of())
                .build();
    }

    private AppointmentRequestDto createTestAppointmentRequest() {
        return AppointmentRequestDto.builder()
                .vehicleId("vehicle-1")
                .serviceType("Oil Change")
                .requestedDateTime(LocalDateTime.now().plusDays(30))
                .specialInstructions("Check tire pressure")
                .build();
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void bookAppointment_Success() throws Exception {
        // Given
        AppointmentRequestDto request = createTestAppointmentRequest();
        AppointmentResponseDto response = createTestAppointmentResponse();

        when(appointmentService.bookAppointment(org.mockito.ArgumentMatchers.any(AppointmentRequestDto.class),
                eq("customer-1")))
                .thenReturn(response); // When & Then
        mockMvc.perform(post("/appointments")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("apt-1"))
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.serviceType").value("Oil Change"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.confirmationNumber").value("APT-2025-001000"));

        verify(appointmentService).bookAppointment(org.mockito.ArgumentMatchers.any(AppointmentRequestDto.class),
                eq("customer-1"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void bookAppointment_UnauthorizedRole_Forbidden() throws Exception {
        // Given
        AppointmentRequestDto request = createTestAppointmentRequest();

        // When & Then
        mockMvc.perform(post("/appointments")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).bookAppointment(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void bookAppointment_InvalidRequest_BadRequest() throws Exception {
        // Given - invalid request with missing required fields
        AppointmentRequestDto invalidRequest = AppointmentRequestDto.builder().build();

        // When & Then
        mockMvc.perform(post("/appointments")
                .header("X-User-Subject", "customer-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
                .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(appointmentService, never()).bookAppointment(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void listAppointments_Customer_Success() throws Exception {
        // Given
        List<AppointmentResponseDto> appointments = List.of(createTestAppointmentResponse());
        when(appointmentService.getAppointmentsForUser("customer-1", "ROLE_CUSTOMER"))
                .thenReturn(appointments);

        // When & Then
        mockMvc.perform(get("/appointments")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("apt-1"))
                .andExpect(jsonPath("$[0].customerId").value("customer-1"));

        verify(appointmentService).getAppointmentsForUser("customer-1", "ROLE_CUSTOMER");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void listAppointments_WithFilters_Success() throws Exception {
        // Given
        List<AppointmentResponseDto> appointments = List.of(createTestAppointmentResponse());
        when(appointmentService.getAppointmentsWithFilters(
                isNull(), eq("vehicle-1"), eq(AppointmentStatus.PENDING),
                eq(LocalDate.of(2025, 6, 1)), eq(LocalDate.of(2025, 6, 30))))
                .thenReturn(appointments);

        // When & Then
        mockMvc.perform(get("/appointments")
                .header("X-User-Subject", "admin-1")
                .header("X-User-Roles", "ROLE_ADMIN")
                .param("vehicleId", "vehicle-1")
                .param("status", "PENDING")
                .param("fromDate", "2025-06-01")
                .param("toDate", "2025-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].vehicleId").value("vehicle-1"));

        verify(appointmentService).getAppointmentsWithFilters(
                isNull(), eq("vehicle-1"), eq(AppointmentStatus.PENDING),
                eq(LocalDate.of(2025, 6, 1)), eq(LocalDate.of(2025, 6, 30)));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void getAppointmentDetails_Success() throws Exception {
        // Given
        AppointmentResponseDto appointment = createTestAppointmentResponse();
        when(appointmentService.getAppointmentDetails("apt-1", "customer-1", "ROLE_CUSTOMER"))
                .thenReturn(appointment);

        // When & Then
        mockMvc.perform(get("/appointments/apt-1")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"))
                .andExpect(jsonPath("$.customerId").value("customer-1"));

        verify(appointmentService).getAppointmentDetails("apt-1", "customer-1", "ROLE_CUSTOMER");
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void updateAppointment_Success() throws Exception {
        // Given
        AppointmentUpdateDto updateDto = AppointmentUpdateDto.builder()
                .requestedDateTime(LocalDateTime.now().plusDays(30))
                .specialInstructions("Updated instructions")
                .build();

        AppointmentResponseDto updatedResponse = createTestAppointmentResponse();
        when(appointmentService.updateAppointment(eq("apt-1"),
                org.mockito.ArgumentMatchers.any(AppointmentUpdateDto.class), eq("customer-1")))
                .thenReturn(updatedResponse); // When & Then
        mockMvc.perform(put("/appointments/apt-1")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"));

        verify(appointmentService).updateAppointment(eq("apt-1"),
                org.mockito.ArgumentMatchers.any(AppointmentUpdateDto.class), eq("customer-1"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void updateAppointment_UnauthorizedRole_Forbidden() throws Exception {
        // Given
        AppointmentUpdateDto updateDto = AppointmentUpdateDto.builder()
                .specialInstructions("Updated by employee")
                .build();

        // When & Then
        mockMvc.perform(put("/appointments/apt-1")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto))
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).updateAppointment(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void cancelAppointment_Success() throws Exception {
        // Given
        doNothing().when(appointmentService).cancelAppointment("apt-1", "customer-1", "ROLE_CUSTOMER");

        // When & Then
        mockMvc.perform(delete("/appointments/apt-1")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "ROLE_CUSTOMER")
                .with(csrf()))
                .andExpect(status().isNoContent());

        verify(appointmentService).cancelAppointment("apt-1", "customer-1", "ROLE_CUSTOMER");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void updateStatus_Success() throws Exception {
        // Given
        StatusUpdateDto statusUpdate = StatusUpdateDto.builder()
                .newStatus(AppointmentStatus.CONFIRMED)
                .build();

        AppointmentResponseDto updatedResponse = createTestAppointmentResponse();
        when(appointmentService.updateAppointmentStatus("apt-1", AppointmentStatus.CONFIRMED, "employee-1"))
                .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(patch("/appointments/apt-1/status")
                .header("X-User-Subject", "admin-1")
                .header("X-User-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"));

        verify(appointmentService).updateAppointmentStatus("apt-1", AppointmentStatus.CONFIRMED, "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void updateStatus_UnauthorizedRole_Forbidden() throws Exception {
        // Given
        StatusUpdateDto statusUpdate = StatusUpdateDto.builder()
                .newStatus(AppointmentStatus.CONFIRMED)
                .build();

        // When & Then
        mockMvc.perform(patch("/appointments/apt-1/status")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate))
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).updateAppointmentStatus(any(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ANONYMOUS")
    void checkAvailability_PublicEndpoint_Success() throws Exception {
        // Given
        AvailabilityResponseDto availability = AvailabilityResponseDto.builder()
                .date(LocalDate.of(2025, 6, 15))
                .serviceType("Oil Change")
                .durationMinutes(60)
                .availableSlots(List.of())
                .build();

        when(appointmentService.checkAvailability(LocalDate.of(2025, 6, 15), "Oil Change", 60))
                .thenReturn(availability);

        // When & Then
        mockMvc.perform(get("/appointments/availability")
                .param("date", "2025-06-15")
                .param("serviceType", "Oil Change")
                .param("duration", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2025-06-15"))
                .andExpect(jsonPath("$.serviceType").value("Oil Change"))
                .andExpect(jsonPath("$.durationMinutes").value(60));

        verify(appointmentService).checkAvailability(LocalDate.of(2025, 6, 15), "Oil Change", 60);
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void getEmployeeSchedule_Success() throws Exception {
        // Given
        ScheduleResponseDto schedule = ScheduleResponseDto.builder()
                .employeeId("employee-1")
                .date(LocalDate.of(2025, 6, 15))
                .appointments(List.of())
                .build();

        when(appointmentService.getEmployeeSchedule("employee-1", LocalDate.of(2025, 6, 15)))
                .thenReturn(schedule);

        // When & Then
        mockMvc.perform(get("/appointments/schedule")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .param("date", "2025-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("employee-1"))
                .andExpect(jsonPath("$.date").value("2025-06-15"));

        verify(appointmentService).getEmployeeSchedule("employee-1", LocalDate.of(2025, 6, 15));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void getEmployeeSchedule_UnauthorizedRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(get("/appointments/schedule")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER")
                .param("date", "2025-06-15"))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).getEmployeeSchedule(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getMonthlyCalendar_Success() throws Exception {
        // Given
        CalendarResponseDto calendar = CalendarResponseDto.builder()
                .month(YearMonth.of(2025, 6))
                .days(List.of())
                .statistics(CalendarStatisticsDto.builder().totalAppointments(0).build())
                .build();

        when(appointmentService.getMonthlyCalendar(YearMonth.of(2025, 6), "ROLE_ADMIN"))
                .thenReturn(calendar);

        // When & Then
        mockMvc.perform(get("/appointments/calendar")
                .header("X-User-Roles", "ROLE_ADMIN")
                .param("year", "2025")
                .param("month", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2025-06"));

        verify(appointmentService).getMonthlyCalendar(YearMonth.of(2025, 6), "ROLE_ADMIN");
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void assignEmployees_Success() throws Exception {
        // Given
        AssignEmployeesRequestDto request = AssignEmployeesRequestDto.builder()
                .employeeIds(Set.of("emp-1", "emp-2"))
                .build();

        AppointmentResponseDto response = createTestAppointmentResponse();
        when(appointmentService.assignEmployees("apt-1", Set.of("emp-1", "emp-2"), "admin-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/appointments/apt-1/assign-employees")
                .header("X-User-Subject", "admin-1")
                .header("X-User-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"));

        verify(appointmentService).assignEmployees("apt-1", Set.of("emp-1", "emp-2"), "admin-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void acceptVehicleArrival_Success() throws Exception {
        // Given
        AppointmentResponseDto response = createTestAppointmentResponse();
        when(appointmentService.acceptVehicleArrival("apt-1", "employee-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/appointments/apt-1/accept-vehicle")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"));

        verify(appointmentService).acceptVehicleArrival("apt-1", "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void completeWork_Success() throws Exception {
        // Given
        AppointmentResponseDto response = createTestAppointmentResponse();
        when(appointmentService.completeWork("apt-1", "employee-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/appointments/apt-1/clock-out")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"));

        verify(appointmentService).completeWork("apt-1", "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void clockIn_Success() throws Exception {
        // Given
        TimeSessionResponse response = new TimeSessionResponse();
        response.setId("session-1");
        response.setAppointmentId("apt-1");
        response.setEmployeeId("employee-1");
        response.setActive(true);
        response.setElapsedSeconds(0L);

        when(appointmentService.clockIn("apt-1", "employee-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/appointments/apt-1/clock-in")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.appointmentId").value("apt-1"))
                .andExpect(jsonPath("$.active").value(true));

        verify(appointmentService).clockIn("apt-1", "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void clockOut_Success() throws Exception {
        // Given
        TimeSessionResponse response = new TimeSessionResponse();
        response.setId("session-1");
        response.setAppointmentId("apt-1");
        response.setEmployeeId("employee-1");
        response.setActive(false);
        response.setElapsedSeconds(7200L); // 2 hours
        response.setHoursWorked(2.0);

        when(appointmentService.clockOut("apt-1", "employee-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/appointments/apt-1/clock-out")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.hoursWorked").value(2.0));

        verify(appointmentService).clockOut("apt-1", "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void getActiveTimeSession_Found_Success() throws Exception {
        // Given
        TimeSessionResponse response = new TimeSessionResponse();
        response.setId("session-1");
        response.setAppointmentId("apt-1");
        response.setActive(true);

        when(appointmentService.getActiveTimeSession("apt-1", "employee-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/appointments/apt-1/time-session")
                .header("X-User-Subject", "employee-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("session-1"))
                .andExpect(jsonPath("$.active").value(true));

        verify(appointmentService).getActiveTimeSession("apt-1", "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void getActiveTimeSession_NotFound_NoContent() throws Exception {
        // Given
        when(appointmentService.getActiveTimeSession("apt-1", "employee-1"))
                .thenReturn(null);

        // When & Then
        mockMvc.perform(get("/appointments/apt-1/time-session")
                .header("X-User-Subject", "employee-1"))
                .andExpect(status().isNoContent());

        verify(appointmentService).getActiveTimeSession("apt-1", "employee-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void confirmCompletion_Success() throws Exception {
        // Given
        AppointmentResponseDto response = createTestAppointmentResponse();
        when(appointmentService.confirmCompletion("apt-1", "customer-1"))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/appointments/apt-1/confirm-completion")
                .header("X-User-Subject", "customer-1")
                .header("X-User-Roles", "CUSTOMER")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("apt-1"));

        verify(appointmentService).confirmCompletion("apt-1", "customer-1");
    }

    @Test
    @WithMockUser(authorities = "ROLE_EMPLOYEE")
    void confirmCompletion_UnauthorizedRole_Forbidden() throws Exception {
        // When & Then
        mockMvc.perform(post("/appointments/apt-1/confirm-completion")
                .header("X-User-Subject", "employee-1")
                .header("X-User-Roles", "EMPLOYEE")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(appointmentService, never()).confirmCompletion(any(), any());
    }

    @Test
    void unauthenticatedRequest_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/appointments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CUSTOMER")
    void checkAvailability_InvalidDate_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/appointments/availability")
                .param("date", "invalid-date")
                .param("serviceType", "Oil Change")
                .param("duration", "60"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getMonthlyCalendar_InvalidMonth_BadRequest() throws Exception {
        // When & Then
        mockMvc.perform(get("/appointments/calendar")
                .header("X-User-Roles", "ROLE_ADMIN")
                .param("year", "2025")
                .param("month", "13")) // Invalid month
                .andExpect(status().isBadRequest());
    }
}