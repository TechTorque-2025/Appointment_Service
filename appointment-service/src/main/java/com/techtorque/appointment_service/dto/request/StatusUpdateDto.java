package com.techtorque.appointment_service.dto.request;

import com.techtorque.appointment_service.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateDto {

  @NotNull(message = "New status is required")
  private AppointmentStatus newStatus;
}
