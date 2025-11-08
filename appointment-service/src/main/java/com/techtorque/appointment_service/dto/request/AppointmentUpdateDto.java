package com.techtorque.appointment_service.dto.request;

import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentUpdateDto {

  @Future(message = "Appointment must be scheduled for a future date and time")
  private LocalDateTime requestedDateTime;

  private String specialInstructions;
}
