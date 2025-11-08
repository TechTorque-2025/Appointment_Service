package com.techtorque.appointment_service.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
  private String serviceType;

  @NotNull(message = "Requested date and time is required")
  @Future(message = "Appointment must be scheduled for a future date and time")
  private LocalDateTime requestedDateTime;

  private String specialInstructions;
}
