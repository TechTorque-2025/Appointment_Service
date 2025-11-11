package com.techtorque.appointment_service.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignEmployeesRequestDto {

  @NotEmpty(message = "At least one employee must be assigned")
  private Set<String> employeeIds;
}
