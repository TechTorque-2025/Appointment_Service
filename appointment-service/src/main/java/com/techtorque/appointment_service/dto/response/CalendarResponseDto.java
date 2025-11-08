package com.techtorque.appointment_service.dto.response;

import com.techtorque.appointment_service.entity.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarResponseDto {

  private YearMonth month;
  private List<CalendarDayDto> days;
  private CalendarStatisticsDto statistics;
}
