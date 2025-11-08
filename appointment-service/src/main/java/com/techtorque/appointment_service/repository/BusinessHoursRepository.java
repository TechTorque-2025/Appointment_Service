package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.BusinessHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.DayOfWeek;
import java.util.Optional;

@Repository
public interface BusinessHoursRepository extends JpaRepository<BusinessHours, String> {

  Optional<BusinessHours> findByDayOfWeek(DayOfWeek dayOfWeek);
}
