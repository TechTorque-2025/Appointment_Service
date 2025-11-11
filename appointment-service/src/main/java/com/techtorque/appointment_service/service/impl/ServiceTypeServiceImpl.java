package com.techtorque.appointment_service.service.impl;

import com.techtorque.appointment_service.dto.request.ServiceTypeRequestDto;
import com.techtorque.appointment_service.dto.response.ServiceTypeResponseDto;
import com.techtorque.appointment_service.service.ServiceTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ServiceTypeServiceImpl implements ServiceTypeService {

  private final WebClient adminServiceWebClient;

  public ServiceTypeServiceImpl(@Qualifier("adminServiceWebClient") WebClient adminServiceWebClient) {
    this.adminServiceWebClient = adminServiceWebClient;
  }

  @Override
  public List<ServiceTypeResponseDto> getAllServiceTypes(boolean includeInactive) {
    log.info("Fetching all service types from Admin Service (includeInactive={})", includeInactive);

    try {
      // Call Admin Service public endpoint to get active service types
      // Note: We ignore includeInactive parameter because public endpoint only returns active types
      List<ServiceTypeResponseDto> serviceTypes = adminServiceWebClient.get()
          .uri("/public/service-types")
          .retrieve()
          .bodyToFlux(ServiceTypeResponseDto.class)
          .collectList()
          .block();

      log.info("Retrieved {} service types from Admin Service", serviceTypes != null ? serviceTypes.size() : 0);
      return serviceTypes != null ? serviceTypes : List.of();
    } catch (Exception e) {
      log.error("Failed to fetch service types from Admin Service", e);
      throw new RuntimeException("Failed to fetch service types: " + e.getMessage());
    }
  }

  @Override
  public ServiceTypeResponseDto getServiceTypeById(String id) {
    log.info("Fetching service type with ID from Admin Service: {}", id);

    try {
      ServiceTypeResponseDto serviceType = adminServiceWebClient.get()
          .uri("/public/service-types/" + id)
          .retrieve()
          .bodyToMono(ServiceTypeResponseDto.class)
          .block();

      if (serviceType == null) {
        throw new IllegalArgumentException("Service type not found with ID: " + id);
      }

      return serviceType;
    } catch (Exception e) {
      log.error("Failed to fetch service type from Admin Service", e);
      throw new RuntimeException("Failed to fetch service type: " + e.getMessage());
    }
  }

  @Override
  public ServiceTypeResponseDto createServiceType(ServiceTypeRequestDto dto) {
    log.info("Creating service type in Admin Service: {}", dto.getName());

    try {
      ServiceTypeResponseDto created = adminServiceWebClient.post()
          .uri("/admin/service-types")
          .bodyValue(dto)
          .retrieve()
          .bodyToMono(ServiceTypeResponseDto.class)
          .block();

      log.info("Service type created successfully in Admin Service");
      return created;
    } catch (Exception e) {
      log.error("Failed to create service type in Admin Service", e);
      throw new RuntimeException("Failed to create service type: " + e.getMessage());
    }
  }

  @Override
  public ServiceTypeResponseDto updateServiceType(String id, ServiceTypeRequestDto dto) {
    log.info("Updating service type in Admin Service with ID: {}", id);

    try {
      ServiceTypeResponseDto updated = adminServiceWebClient.put()
          .uri("/admin/service-types/" + id)
          .bodyValue(dto)
          .retrieve()
          .bodyToMono(ServiceTypeResponseDto.class)
          .block();

      log.info("Service type updated successfully in Admin Service");
      return updated;
    } catch (Exception e) {
      log.error("Failed to update service type in Admin Service", e);
      throw new RuntimeException("Failed to update service type: " + e.getMessage());
    }
  }

  @Override
  public void deleteServiceType(String id) {
    log.info("Deleting service type in Admin Service with ID: {}", id);

    try {
      adminServiceWebClient.delete()
          .uri("/admin/service-types/" + id)
          .retrieve()
          .bodyToMono(Void.class)
          .block();

      log.info("Service type deleted successfully in Admin Service");
    } catch (Exception e) {
      log.error("Failed to delete service type in Admin Service", e);
      throw new RuntimeException("Failed to delete service type: " + e.getMessage());
    }
  }

  @Override
  public List<ServiceTypeResponseDto> getServiceTypesByCategory(String category) {
    log.info("Fetching service types by category from Admin Service: {}", category);

    try {
      // Get all active service types and filter by category
      List<ServiceTypeResponseDto> allServiceTypes = getAllServiceTypes(false);
      return allServiceTypes.stream()
          .filter(st -> category.equalsIgnoreCase(st.getCategory()))
          .toList();
    } catch (Exception e) {
      log.error("Failed to fetch service types by category", e);
      throw new RuntimeException("Failed to fetch service types by category: " + e.getMessage());
    }
  }
}
