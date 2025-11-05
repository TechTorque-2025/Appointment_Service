package com.techtorque.appointment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDto {

  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private boolean available;
  private String bayId;
  private String bayName;
}
