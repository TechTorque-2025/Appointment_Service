package com.techtorque.appointment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarStatisticsDto {

  private int totalAppointments;
  private int completedAppointments;
  private int pendingAppointments;
  private int confirmedAppointments;
  private int cancelledAppointments;
  private Map<String, Integer> appointmentsByServiceType;
  private Map<String, Integer> appointmentsByBay;
}
