package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.dto.request.ServiceTypeRequestDto;
import com.techtorque.appointment_service.dto.response.ServiceTypeResponseDto;
import java.util.List;

public interface ServiceTypeService {

  List<ServiceTypeResponseDto> getAllServiceTypes(boolean includeInactive);

  ServiceTypeResponseDto getServiceTypeById(String id);

  ServiceTypeResponseDto createServiceType(ServiceTypeRequestDto dto);

  ServiceTypeResponseDto updateServiceType(String id, ServiceTypeRequestDto dto);

  void deleteServiceType(String id);

  List<ServiceTypeResponseDto> getServiceTypesByCategory(String category);
}
