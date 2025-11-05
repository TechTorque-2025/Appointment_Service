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
public class CalendarDayDto {

  private LocalDate date;
  private int appointmentCount;
  private boolean isHoliday;
  private String holidayName;
  private List<AppointmentSummaryDto> appointments;
}
