package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

  // For customers to view their appointments
  List<Appointment> findByCustomerIdOrderByRequestedDateTimeDesc(String customerId);

  // For employees to view their assigned appointments
  List<Appointment> findByAssignedEmployeeIdAndRequestedDateTimeBetween(String assignedEmployeeId, LocalDateTime start, LocalDateTime end);

  // For checking general availability
  List<Appointment> findByRequestedDateTimeBetween(LocalDateTime start, LocalDateTime end);

  // For security: find an appointment by ID only if it belongs to the specified customer
  Optional<Appointment> findByIdAndCustomerId(String id, String customerId);
}