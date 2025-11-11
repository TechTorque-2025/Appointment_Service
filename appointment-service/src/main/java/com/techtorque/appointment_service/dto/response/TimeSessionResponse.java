package com.techtorque.appointment_service.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSessionResponse {
  
  private String id;
  private String appointmentId;
  private String employeeId;
  private LocalDateTime clockInTime;
  private LocalDateTime clockOutTime;
  private boolean active;
  private Long elapsedSeconds; // Calculated field for timer
  private Double hoursWorked; // Calculated when clocked out
}
