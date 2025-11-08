package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, String> {

  Optional<Holiday> findByDate(LocalDate date);

  boolean existsByDate(LocalDate date);
}
