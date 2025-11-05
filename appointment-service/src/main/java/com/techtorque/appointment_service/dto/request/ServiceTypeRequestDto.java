package com.techtorque.appointment_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTypeRequestDto {

  @NotBlank(message = "Service name is required")
  private String name;

  @NotBlank(message = "Category is required")
  private String category;

  @NotNull(message = "Base price is required")
  @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
  private BigDecimal basePriceLKR;

  @NotNull(message = "Estimated duration is required")
  @Min(value = 1, message = "Duration must be at least 1 minute")
  private Integer estimatedDurationMinutes;

  private String description;

  @Builder.Default
  private Boolean active = true;
}
