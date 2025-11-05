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
public class AppointmentResponseDto {

  private String id;
  private String customerId;
  private String vehicleId;
  private String assignedEmployeeId;
  private String serviceType;
  private LocalDateTime requestedDateTime;
  private AppointmentStatus status;
  private String specialInstructions;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
