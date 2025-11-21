package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.Appointment;
import com.techtorque.appointment_service.entity.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

import com.techtorque.appointment_service.config.TestDataSourceConfig;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
class AppointmentRepositoryTest {

        @Autowired
        private AppointmentRepository appointmentRepository;

        private Appointment testAppointment;
        private LocalDateTime testDateTime;

        @BeforeEach
        void setUp() {
                appointmentRepository.deleteAll();

                testDateTime = LocalDateTime.now().plusDays(1);

                Set<String> employeeIds = new HashSet<>();
                employeeIds.add("emp-123");
                employeeIds.add("emp-456");

                testAppointment = Appointment.builder()
                                .customerId("customer-123")
                                .vehicleId("vehicle-456")
                                .assignedEmployeeIds(employeeIds)
                                .assignedBayId("bay-001")
                                .confirmationNumber("APT-2025-001")
                                .serviceType("Oil Change")
                                .requestedDateTime(testDateTime)
                                .status(AppointmentStatus.PENDING)
                                .specialInstructions("Please check brakes")
                                .build();

                appointmentRepository.save(testAppointment);
        }

        @Test
        void testFindByCustomerIdOrderByRequestedDateTimeDesc() {
                // Create another appointment for the same customer
                Appointment appointment2 = Appointment.builder()
                                .customerId("customer-123")
                                .vehicleId("vehicle-789")
                                .serviceType("Tire Rotation")
                                .requestedDateTime(testDateTime.plusDays(1))
                                .status(AppointmentStatus.CONFIRMED)
                                .build();
                appointmentRepository.save(appointment2);

                List<Appointment> appointments = appointmentRepository
                                .findByCustomerIdOrderByRequestedDateTimeDesc("customer-123");

                assertThat(appointments).hasSize(2);
                assertThat(appointments.get(0).getRequestedDateTime())
                                .isAfter(appointments.get(1).getRequestedDateTime());
        }

        @Test
        void testFindByAssignedEmployeeIdAndRequestedDateTimeBetween() {
                LocalDateTime start = testDateTime.minusHours(1);
                LocalDateTime end = testDateTime.plusHours(1);

                List<Appointment> appointments = appointmentRepository
                                .findByAssignedEmployeeIdAndRequestedDateTimeBetween("emp-123", start, end);

                assertThat(appointments).hasSize(1);
                assertThat(appointments.get(0).getId()).isEqualTo(testAppointment.getId());
        }

        @Test
        void testFindByRequestedDateTimeBetween() {
                LocalDateTime start = testDateTime.minusHours(1);
                LocalDateTime end = testDateTime.plusHours(1);

                List<Appointment> appointments = appointmentRepository
                                .findByRequestedDateTimeBetween(start, end);

                assertThat(appointments).hasSize(1);
                assertThat(appointments.get(0).getId()).isEqualTo(testAppointment.getId());
        }

        @Test
        void testFindByIdAndCustomerId() {
                Optional<Appointment> found = appointmentRepository
                                .findByIdAndCustomerId(testAppointment.getId(), "customer-123");

                assertThat(found).isPresent();
                assertThat(found.get().getId()).isEqualTo(testAppointment.getId());
        }

        @Test
        void testFindByIdAndCustomerId_WrongCustomer() {
                Optional<Appointment> found = appointmentRepository
                                .findByIdAndCustomerId(testAppointment.getId(), "wrong-customer");

                assertThat(found).isEmpty();
        }

        @Test
        void testFindWithFilters_AllFilters() {
                List<Appointment> appointments = appointmentRepository.findWithFilters(
                                "customer-123",
                                "vehicle-456",
                                "PENDING",
                                testDateTime.minusHours(1),
                                testDateTime.plusHours(1));

                assertThat(appointments).hasSize(1);
                assertThat(appointments.get(0).getCustomerId()).isEqualTo("customer-123");
        }

        @Test
        void testFindWithFilters_NullFilters() {
                List<Appointment> appointments = appointmentRepository.findWithFilters(
                                null, null, null, null, null);

                assertThat(appointments).hasSize(1);
        }

        @Test
        void testCountByStatus() {
                // Create appointments with different statuses
                Appointment confirmedAppt = Appointment.builder()
                                .customerId("customer-456")
                                .vehicleId("vehicle-789")
                                .serviceType("Inspection")
                                .requestedDateTime(testDateTime.plusDays(2))
                                .status(AppointmentStatus.CONFIRMED)
                                .build();
                appointmentRepository.save(confirmedAppt);

                long pendingCount = appointmentRepository.countByStatus(AppointmentStatus.PENDING);
                long confirmedCount = appointmentRepository.countByStatus(AppointmentStatus.CONFIRMED);

                assertThat(pendingCount).isEqualTo(1);
                assertThat(confirmedCount).isEqualTo(1);
        }

        @Test
        void testFindByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot() {
                LocalDateTime start = testDateTime.minusHours(1);
                LocalDateTime end = testDateTime.plusHours(1);

                List<Appointment> appointments = appointmentRepository
                                .findByAssignedBayIdAndRequestedDateTimeBetweenAndStatusNot(
                                                "bay-001", start, end, AppointmentStatus.CANCELLED);

                assertThat(appointments).hasSize(1);
                assertThat(appointments.get(0).getAssignedBayId()).isEqualTo("bay-001");
        }

        @Test
        void testFindMaxConfirmationNumberByPrefix() {
                // Create appointments with sequential confirmation numbers
                Appointment appt2 = Appointment.builder()
                                .customerId("customer-789")
                                .vehicleId("vehicle-101")
                                .confirmationNumber("APT-2025-002")
                                .serviceType("Brake Service")
                                .requestedDateTime(testDateTime.plusDays(3))
                                .status(AppointmentStatus.PENDING)
                                .build();
                appointmentRepository.save(appt2);

                Optional<String> maxNumber = appointmentRepository
                                .findMaxConfirmationNumberByPrefix("APT-2025-");

                assertThat(maxNumber).isPresent();
                assertThat(maxNumber.get()).isEqualTo("APT-2025-002");
        }

        @Test
        void testSaveAndRetrieve() {
                Appointment newAppointment = Appointment.builder()
                                .customerId("customer-new")
                                .vehicleId("vehicle-new")
                                .serviceType("Full Service")
                                .requestedDateTime(testDateTime.plusDays(5))
                                .status(AppointmentStatus.PENDING)
                                .build();

                Appointment saved = appointmentRepository.save(newAppointment);

                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getCustomerId()).isEqualTo("customer-new");
                assertThat(saved.getServiceType()).isEqualTo("Full Service");
                assertThat(saved.getStatus()).isEqualTo(AppointmentStatus.PENDING);
        }
}
