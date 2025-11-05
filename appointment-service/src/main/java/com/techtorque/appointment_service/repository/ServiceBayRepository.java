package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.ServiceBay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceBayRepository extends JpaRepository<ServiceBay, String> {

  List<ServiceBay> findByActiveTrue();

  List<ServiceBay> findByActiveTrueOrderByBayNumberAsc();
}
