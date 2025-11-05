package com.techtorque.appointment_service.dto;

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
public class ScheduleItemDto {

  private String appointmentId;
  private String customerId;
  private String vehicleId;
  private String serviceType;
  private LocalDateTime startTime;
  private AppointmentStatus status;
  private String specialInstructions;
}
