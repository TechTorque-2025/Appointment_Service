package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceTypeRepository extends JpaRepository<ServiceType, String> {

  List<ServiceType> findByActiveTrue();

  Optional<ServiceType> findByNameAndActiveTrue(String name);

  List<ServiceType> findByCategoryAndActiveTrue(String category);
}
