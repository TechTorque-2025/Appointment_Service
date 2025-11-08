package com.techtorque.appointment_service.dto.response;

import com.techtorque.appointment_service.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentSummaryDto {

  private String id;
  private String confirmationNumber;
  private LocalDateTime time;
  private String serviceType;
  private AppointmentStatus status;
  private String bayName;
}
