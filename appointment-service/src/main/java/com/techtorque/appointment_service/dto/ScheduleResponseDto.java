package com.techtorque.appointment_service.dto;

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
public class ScheduleResponseDto {

  private String employeeId;
  private LocalDate date;
  private List<ScheduleItemDto> appointments;
}
