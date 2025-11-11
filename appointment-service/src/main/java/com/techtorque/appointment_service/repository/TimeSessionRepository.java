package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.TimeSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSessionRepository extends JpaRepository<TimeSession, String> {
  
  Optional<TimeSession> findByAppointmentIdAndActiveTrue(String appointmentId);
  
  Optional<TimeSession> findByAppointmentIdAndEmployeeIdAndActiveTrue(String appointmentId, String employeeId);
  
  List<TimeSession> findByEmployeeIdAndActiveTrue(String employeeId);
  
  List<TimeSession> findByEmployeeIdOrderByClockInTimeDesc(String employeeId);
}
