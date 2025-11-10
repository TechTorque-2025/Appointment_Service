package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {

  // For customers to view their appointments
  List<Appointment> findByCustomerIdOrderByRequestedDateTimeDesc(String customerId);

  // For employees to view their assigned appointments
  @Query("SELECT a FROM Appointment a JOIN a.assignedEmployeeIds e WHERE e = :employeeId AND a.requestedDateTime BETWEEN :start AND :end")
  List<Appointment> findByAssignedEmployeeIdAndRequestedDateTimeBetween(
      @Param("employeeId") String employeeId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // For checking general availability
  List<Appointment> findByRequestedDateTimeBetween(LocalDateTime start, LocalDateTime end);

  // For security: find an appointment by ID only if it belongs to the specified customer
  Optional<Appointment> findByIdAndCustomerId(String id, String customerId);

  // Query with filters
  @Query(value = "SELECT * FROM appointments a WHERE " +
         "(CAST(:customerId AS text) IS NULL OR a.customer_id = :customerId) AND " +
         "(CAST(:vehicleId AS text) IS NULL OR a.vehicle_id = :vehicleId) AND " +
         "(CAST(:status AS text) IS NULL OR a.status = :status) AND " +
         "(CAST(:fromDate AS timestamp) IS NULL OR a.requested_date_time >= :fromDate) AND " +
         "(CAST(:toDate AS timestamp) IS NULL OR a.requested_date_time <= :toDate) " +
         "ORDER BY a.requested_date_time DESC", 
         nativeQuery = true)
  List<Appointment> findWithFilters(
      @Param("customerId") String customerId,
      @Param("vehicleId") String vehicleId,
      @Param("status") String status,
      @Param("fromDate") LocalDateTime fromDate,
      @Param("toDate") LocalDateTime toDate);

  // Count appointments by status
  long countByStatus(AppointmentStatus status);

  // Find appointments by bay and time range (for bay availability)
  List<Appointment> findByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot(
      String bayId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);

  // Get next confirmation number
  @Query("SELECT MAX(a.confirmationNumber) FROM Appointment a WHERE a.confirmationNumber LIKE :prefix%")
  Optional<String> findMaxConfirmationNumberByPrefix(@Param("prefix") String prefix);
}
