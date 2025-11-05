package com.techtorque.appointment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponseDto {

  private LocalDate date;
  private String serviceType;
  private int durationMinutes;
  private List<TimeSlotDto> availableSlots;
}
