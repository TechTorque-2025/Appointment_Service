package com.techtorque.appointment_service.dto.response;

import com.techtorque.appointment_service.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {

  private String id;
  private String customerId;
  private String vehicleId;
  private Set<String> assignedEmployeeIds;
  private String assignedBayId;
  private String confirmationNumber;
  private String serviceType;
  private LocalDateTime requestedDateTime;
  private AppointmentStatus status;
  private String specialInstructions;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Vehicle arrival tracking
  private LocalDateTime vehicleArrivedAt;
  private String vehicleAcceptedByEmployeeId;
}
