package com.techtorque.appointment_service.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequestDto {

    @NotBlank(message = "Vehicle ID is required")
    private String vehicleId;

    @NotBlank(message = "Service type is required")
    @Size(min = 3, max = 100, message = "Service type must be between 3 and 100 characters")
    private String serviceType;

    @NotNull(message = "Requested date and time is required")
    @Future(message = "Appointment date must be in the future")
    private LocalDateTime requestedDateTime;

    @Size(max = 500, message = "Special instructions cannot exceed 500 characters")
    private String specialInstructions;
}
