package com.techtorque.appointment_service.config;

import com.techtorque.appointment_service.entity.*;
import com.techtorque.appointment_service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import java.math.BigDecimal;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data seeder for development environment
 * Seeds: ServiceTypes, ServiceBays, BusinessHours, Holidays, and sample Appointments
 */
@Configuration
@Profile("dev")
@Slf4j
public class DataSeeder {

  // Shared constants for cross-service consistency
  // These UUIDs should match the Auth service seeded users
  public static final String CUSTOMER_1_ID = "00000000-0000-0000-0000-000000000101";
  public static final String CUSTOMER_2_ID = "00000000-0000-0000-0000-000000000102";
  public static final String EMPLOYEE_1_ID = "00000000-0000-0000-0000-000000000003";
  public static final String EMPLOYEE_2_ID = "00000000-0000-0000-0000-000000000004";
  public static final String EMPLOYEE_3_ID = "00000000-0000-0000-0000-000000000005";

  // Vehicle IDs (should match Vehicle service seed data)
  public static final String VEHICLE_1_ID = "VEH-001";
  public static final String VEHICLE_2_ID = "VEH-002";
  public static final String VEHICLE_3_ID = "VEH-003";
  public static final String VEHICLE_4_ID = "VEH-004";

  @Bean
  CommandLineRunner initDatabase(
      ServiceTypeRepository serviceTypeRepository,
      ServiceBayRepository serviceBayRepository,
      BusinessHoursRepository businessHoursRepository,
      HolidayRepository holidayRepository,
      AppointmentRepository appointmentRepository) {
    return args -> {
      log.info("Starting data seeding for Appointment Service (dev profile)...");

      // 1. Seed Service Types
      seedServiceTypes(serviceTypeRepository);

      // 2. Seed Service Bays
      seedServiceBays(serviceBayRepository);

      // 3. Seed Business Hours
      seedBusinessHours(businessHoursRepository);

      // 4. Seed Holidays
      seedHolidays(holidayRepository);

      // 5. Seed Sample Appointments
      seedAppointments(appointmentRepository, serviceBayRepository);

      log.info("Data seeding completed successfully!");
    };
  }

  private void seedServiceTypes(ServiceTypeRepository repository) {
    if (repository.count() > 0) {
      log.info("Service types already exist. Skipping seeding.");
      return;
    }

    log.info("Seeding service types...");

    List<ServiceType> serviceTypes = List.of(
        ServiceType.builder()
            .name("Oil Change")
            .category("Maintenance")
            .basePriceLKR(new BigDecimal("5000.00"))
            .estimatedDurationMinutes(30)
            .description("Complete oil and filter change service")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Brake Service")
            .category("Maintenance")
            .basePriceLKR(new BigDecimal("12000.00"))
            .estimatedDurationMinutes(90)
            .description("Brake pad replacement and brake system inspection")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Tire Rotation")
            .category("Maintenance")
            .basePriceLKR(new BigDecimal("3000.00"))
            .estimatedDurationMinutes(30)
            .description("Four-wheel tire rotation and balance")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Wheel Alignment")
            .category("Maintenance")
            .basePriceLKR(new BigDecimal("4500.00"))
            .estimatedDurationMinutes(60)
            .description("Four-wheel alignment and suspension check")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Engine Diagnostic")
            .category("Repair")
            .basePriceLKR(new BigDecimal("8000.00"))
            .estimatedDurationMinutes(120)
            .description("Comprehensive engine diagnostic and fault code reading")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Battery Replacement")
            .category("Repair")
            .basePriceLKR(new BigDecimal("15000.00"))
            .estimatedDurationMinutes(45)
            .description("Battery replacement and electrical system check")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("AC Service")
            .category("Maintenance")
            .basePriceLKR(new BigDecimal("7500.00"))
            .estimatedDurationMinutes(60)
            .description("Air conditioning system service and refrigerant recharge")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Full Service")
            .category("Maintenance")
            .basePriceLKR(new BigDecimal("25000.00"))
            .estimatedDurationMinutes(180)
            .description("Comprehensive vehicle service including oil, filters, and inspection")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Paint Protection")
            .category("Modification")
            .basePriceLKR(new BigDecimal("35000.00"))
            .estimatedDurationMinutes(240)
            .description("Ceramic coating and paint protection application")
            .active(true)
            .build(),
        
        ServiceType.builder()
            .name("Custom Exhaust")
            .category("Modification")
            .basePriceLKR(new BigDecimal("50000.00"))
            .estimatedDurationMinutes(300)
            .description("Custom exhaust system installation")
            .active(true)
            .build()
    );

    repository.saveAll(serviceTypes);
    log.info("Seeded {} service types", serviceTypes.size());
  }

  private void seedServiceBays(ServiceBayRepository repository) {
    if (repository.count() > 0) {
      log.info("Service bays already exist. Skipping seeding.");
      return;
    }

    log.info("Seeding service bays...");

    List<ServiceBay> bays = List.of(
        ServiceBay.builder()
            .bayNumber("BAY-01")
            .name("Bay 1 - Quick Service")
            .description("For quick maintenance services like oil changes and tire rotations")
            .capacity(1)
            .active(true)
            .build(),
        
        ServiceBay.builder()
            .bayNumber("BAY-02")
            .name("Bay 2 - General Repair")
            .description("For general repair and maintenance work")
            .capacity(1)
            .active(true)
            .build(),
        
        ServiceBay.builder()
            .bayNumber("BAY-03")
            .name("Bay 3 - Diagnostic")
            .description("Equipped with diagnostic tools for engine and electrical diagnostics")
            .capacity(1)
            .active(true)
            .build(),
        
        ServiceBay.builder()
            .bayNumber("BAY-04")
            .name("Bay 4 - Modification")
            .description("For custom modifications and paint work")
            .capacity(1)
            .active(true)
            .build()
    );

    repository.saveAll(bays);
    log.info("Seeded {} service bays", bays.size());
  }

  private void seedBusinessHours(BusinessHoursRepository repository) {
    if (repository.count() > 0) {
      log.info("Business hours already exist. Skipping seeding.");
      return;
    }

    log.info("Seeding business hours...");

    List<BusinessHours> businessHours = new ArrayList<>();

    // Monday to Friday: 8 AM - 6 PM with lunch break 12-1 PM
    for (DayOfWeek day : List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                                   DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)) {
      businessHours.add(BusinessHours.builder()
          .dayOfWeek(day)
          .openTime(LocalTime.of(8, 0))
          .closeTime(LocalTime.of(18, 0))
          .breakStartTime(LocalTime.of(12, 0))
          .breakEndTime(LocalTime.of(13, 0))
          .isOpen(true)
          .build());
    }

    // Saturday: 9 AM - 3 PM (no break)
    businessHours.add(BusinessHours.builder()
        .dayOfWeek(DayOfWeek.SATURDAY)
        .openTime(LocalTime.of(9, 0))
        .closeTime(LocalTime.of(15, 0))
        .isOpen(true)
        .build());

    // Sunday: Closed
    businessHours.add(BusinessHours.builder()
        .dayOfWeek(DayOfWeek.SUNDAY)
        .openTime(LocalTime.of(0, 0))
        .closeTime(LocalTime.of(0, 0))
        .isOpen(false)
        .build());

    repository.saveAll(businessHours);
    log.info("Seeded business hours for all days of the week");
  }

  private void seedHolidays(HolidayRepository repository) {
    if (repository.count() > 0) {
      log.info("Holidays already exist. Skipping seeding.");
      return;
    }

    log.info("Seeding holidays...");

    int currentYear = LocalDate.now().getYear();

    List<Holiday> holidays = List.of(
        Holiday.builder()
            .date(LocalDate.of(currentYear, 1, 1))
            .name("New Year's Day")
            .description("National Holiday")
            .build(),
        
        Holiday.builder()
            .date(LocalDate.of(currentYear, 2, 4))
            .name("Independence Day")
            .description("National Holiday")
            .build(),
        
        Holiday.builder()
            .date(LocalDate.of(currentYear, 5, 1))
            .name("May Day")
            .description("National Holiday")
            .build(),
        
        Holiday.builder()
            .date(LocalDate.of(currentYear, 12, 25))
            .name("Christmas Day")
            .description("National Holiday")
            .build()
    );

    repository.saveAll(holidays);
    log.info("Seeded {} holidays", holidays.size());
  }

  private void seedAppointments(AppointmentRepository appointmentRepository, ServiceBayRepository serviceBayRepository) {
    if (appointmentRepository.count() > 0) {
      log.info("Appointments already exist. Skipping seeding.");
      return;
    }

    log.info("Seeding sample appointments...");

    List<ServiceBay> bays = serviceBayRepository.findAll();
    if (bays.isEmpty()) {
      log.warn("No service bays found. Cannot seed appointments.");
      return;
    }

    LocalDate today = LocalDate.now();
    int confirmationCounter = 1000;

    List<Appointment> appointments = new ArrayList<>();

    // Past appointment - COMPLETED
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_1_ID)
        .vehicleId(VEHICLE_1_ID)
        .assignedEmployeeId(EMPLOYEE_1_ID)
        .assignedBayId(bays.get(0).getId())
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Oil Change")
        .requestedDateTime(today.minusDays(7).atTime(10, 0))
        .status(AppointmentStatus.COMPLETED)
        .specialInstructions("Please check tire pressure as well")
        .build());

    // Past appointment - COMPLETED
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_2_ID)
        .vehicleId(VEHICLE_3_ID)
        .assignedEmployeeId(EMPLOYEE_2_ID)
        .assignedBayId(bays.get(1).getId())
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Brake Service")
        .requestedDateTime(today.minusDays(5).atTime(14, 0))
        .status(AppointmentStatus.COMPLETED)
        .specialInstructions("Brake pads were making noise")
        .build());

    // Today's appointment - IN_PROGRESS
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_1_ID)
        .vehicleId(VEHICLE_2_ID)
        .assignedEmployeeId(EMPLOYEE_1_ID)
        .assignedBayId(bays.get(0).getId())
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Wheel Alignment")
        .requestedDateTime(today.atTime(9, 0))
        .status(AppointmentStatus.IN_PROGRESS)
        .specialInstructions("Car pulls to the right")
        .build());

    // Today's appointment - CONFIRMED
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_2_ID)
        .vehicleId(VEHICLE_4_ID)
        .assignedEmployeeId(EMPLOYEE_3_ID)
        .assignedBayId(bays.get(2).getId())
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Engine Diagnostic")
        .requestedDateTime(today.atTime(11, 0))
        .status(AppointmentStatus.CONFIRMED)
        .specialInstructions("Check engine light is on")
        .build());

    // Tomorrow's appointment - CONFIRMED
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_1_ID)
        .vehicleId(VEHICLE_1_ID)
        .assignedEmployeeId(EMPLOYEE_2_ID)
        .assignedBayId(bays.get(1).getId())
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("AC Service")
        .requestedDateTime(today.plusDays(1).atTime(10, 0))
        .status(AppointmentStatus.CONFIRMED)
        .specialInstructions("AC not cooling properly")
        .build());

    // Future appointment - PENDING
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_2_ID)
        .vehicleId(VEHICLE_3_ID)
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Full Service")
        .requestedDateTime(today.plusDays(3).atTime(9, 0))
        .status(AppointmentStatus.PENDING)
        .specialInstructions("Complete service needed, car has 50,000 km")
        .build());

    // Future appointment - PENDING
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_1_ID)
        .vehicleId(VEHICLE_2_ID)
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Tire Rotation")
        .requestedDateTime(today.plusDays(5).atTime(14, 30))
        .status(AppointmentStatus.PENDING)
        .specialInstructions(null)
        .build());

    // Cancelled appointment
    appointments.add(Appointment.builder()
        .customerId(CUSTOMER_1_ID)
        .vehicleId(VEHICLE_1_ID)
        .confirmationNumber("APT-" + today.getYear() + "-" + String.format("%06d", confirmationCounter++))
        .serviceType("Battery Replacement")
        .requestedDateTime(today.plusDays(2).atTime(15, 0))
        .status(AppointmentStatus.CANCELLED)
        .specialInstructions("Battery issue resolved")
        .build());

    appointmentRepository.saveAll(appointments);
    log.info("Seeded {} sample appointments", appointments.size());
  }
}
