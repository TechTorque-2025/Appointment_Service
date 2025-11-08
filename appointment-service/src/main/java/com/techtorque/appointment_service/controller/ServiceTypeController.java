package com.techtorque.appointment_service.controller;

import com.techtorque.appointment_service.dto.response.ServiceTypeResponseDto;
import com.techtorque.appointment_service.dto.request.ServiceTypeRequestDto;
import com.techtorque.appointment_service.service.ServiceTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/service-types")
@Tag(name = "Service Type Management", description = "Endpoints for managing service types (Admin only)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class ServiceTypeController {

  private final ServiceTypeService serviceTypeService;

  public ServiceTypeController(ServiceTypeService serviceTypeService) {
    this.serviceTypeService = serviceTypeService;
  }

  @Operation(summary = "Get all service types")
  @GetMapping
  public ResponseEntity<List<ServiceTypeResponseDto>> getAllServiceTypes(
          @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
    List<ServiceTypeResponseDto> serviceTypes = serviceTypeService.getAllServiceTypes(includeInactive);
    return ResponseEntity.ok(serviceTypes);
  }

  @Operation(summary = "Get service type by ID")
  @GetMapping("/{id}")
  public ResponseEntity<ServiceTypeResponseDto> getServiceTypeById(@PathVariable String id) {
    ServiceTypeResponseDto serviceType = serviceTypeService.getServiceTypeById(id);
    return ResponseEntity.ok(serviceType);
  }

  @Operation(summary = "Create a new service type")
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ServiceTypeResponseDto> createServiceType(
          @Valid @RequestBody ServiceTypeRequestDto dto) {
    ServiceTypeResponseDto created = serviceTypeService.createServiceType(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @Operation(summary = "Update a service type")
  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ServiceTypeResponseDto> updateServiceType(
          @PathVariable String id,
          @Valid @RequestBody ServiceTypeRequestDto dto) {
    ServiceTypeResponseDto updated = serviceTypeService.updateServiceType(id, dto);
    return ResponseEntity.ok(updated);
  }

  @Operation(summary = "Delete (deactivate) a service type")
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteServiceType(@PathVariable String id) {
    serviceTypeService.deleteServiceType(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Get service types by category")
  @GetMapping("/category/{category}")
  public ResponseEntity<List<ServiceTypeResponseDto>> getServiceTypesByCategory(
          @PathVariable String category) {
    List<ServiceTypeResponseDto> serviceTypes = serviceTypeService.getServiceTypesByCategory(category);
    return ResponseEntity.ok(serviceTypes);
  }
}
