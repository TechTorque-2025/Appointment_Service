package com.techtorque.appointment_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTypeResponseDto {

  private String id;
  private String name;
  private String category;
  private BigDecimal basePriceLKR;
  private Integer estimatedDurationMinutes;
  private String description;
  private Boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
