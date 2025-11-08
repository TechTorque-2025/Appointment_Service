package com.techtorque.appointment_service.service.impl;

import com.techtorque.appointment_service.dto.request.ServiceTypeRequestDto;
import com.techtorque.appointment_service.dto.response.ServiceTypeResponseDto;
import com.techtorque.appointment_service.entity.ServiceType;
import com.techtorque.appointment_service.repository.ServiceTypeRepository;
import com.techtorque.appointment_service.service.ServiceTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class ServiceTypeServiceImpl implements ServiceTypeService {

  private final ServiceTypeRepository serviceTypeRepository;

  public ServiceTypeServiceImpl(ServiceTypeRepository serviceTypeRepository) {
    this.serviceTypeRepository = serviceTypeRepository;
  }

  @Override
  public List<ServiceTypeResponseDto> getAllServiceTypes(boolean includeInactive) {
    log.info("Fetching all service types (includeInactive={})", includeInactive);

    List<ServiceType> serviceTypes = includeInactive 
        ? serviceTypeRepository.findAll()
        : serviceTypeRepository.findByActiveTrue();

    return serviceTypes.stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
  }

  @Override
  public ServiceTypeResponseDto getServiceTypeById(String id) {
    log.info("Fetching service type with ID: {}", id);

    ServiceType serviceType = serviceTypeRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID: " + id));

    return convertToDto(serviceType);
  }

  @Override
  public ServiceTypeResponseDto createServiceType(ServiceTypeRequestDto dto) {
    log.info("Creating new service type: {}", dto.getName());

    // Check if service type with same name already exists
    serviceTypeRepository.findByNameAndActiveTrue(dto.getName()).ifPresent(existing -> {
      throw new IllegalArgumentException("Service type with name '" + dto.getName() + "' already exists");
    });

    ServiceType serviceType = ServiceType.builder()
        .name(dto.getName())
        .category(dto.getCategory())
        .basePriceLKR(dto.getBasePriceLKR())
        .estimatedDurationMinutes(dto.getEstimatedDurationMinutes())
        .description(dto.getDescription())
        .active(dto.getActive())
        .build();

    ServiceType saved = serviceTypeRepository.save(serviceType);
    log.info("Service type created successfully with ID: {}", saved.getId());

    return convertToDto(saved);
  }

  @Override
  public ServiceTypeResponseDto updateServiceType(String id, ServiceTypeRequestDto dto) {
    log.info("Updating service type with ID: {}", id);

    ServiceType serviceType = serviceTypeRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID: " + id));

    // Check if new name conflicts with existing service types
    if (!serviceType.getName().equals(dto.getName())) {
      serviceTypeRepository.findByNameAndActiveTrue(dto.getName()).ifPresent(existing -> {
        if (!existing.getId().equals(id)) {
          throw new IllegalArgumentException("Service type with name '" + dto.getName() + "' already exists");
        }
      });
    }

    serviceType.setName(dto.getName());
    serviceType.setCategory(dto.getCategory());
    serviceType.setBasePriceLKR(dto.getBasePriceLKR());
    serviceType.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
    serviceType.setDescription(dto.getDescription());
    serviceType.setActive(dto.getActive());

    ServiceType updated = serviceTypeRepository.save(serviceType);
    log.info("Service type updated successfully: {}", id);

    return convertToDto(updated);
  }

  @Override
  public void deleteServiceType(String id) {
    log.info("Deactivating service type with ID: {}", id);

    ServiceType serviceType = serviceTypeRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Service type not found with ID: " + id));

    serviceType.setActive(false);
    serviceTypeRepository.save(serviceType);

    log.info("Service type deactivated successfully: {}", id);
  }

  @Override
  public List<ServiceTypeResponseDto> getServiceTypesByCategory(String category) {
    log.info("Fetching service types by category: {}", category);

    List<ServiceType> serviceTypes = serviceTypeRepository.findByCategoryAndActiveTrue(category);

    return serviceTypes.stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
  }

  private ServiceTypeResponseDto convertToDto(ServiceType serviceType) {
    return ServiceTypeResponseDto.builder()
        .id(serviceType.getId())
        .name(serviceType.getName())
        .category(serviceType.getCategory())
        .basePriceLKR(serviceType.getBasePriceLKR())
        .estimatedDurationMinutes(serviceType.getEstimatedDurationMinutes())
        .description(serviceType.getDescription())
        .active(serviceType.getActive())
        .createdAt(serviceType.getCreatedAt())
        .updatedAt(serviceType.getUpdatedAt())
        .build();
  }
}
