package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.TimeSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import com.techtorque.appointment_service.config.TestDataSourceConfig;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
class TimeSessionRepositoryTest {

        @Autowired
        private TimeSessionRepository timeSessionRepository;

        private TimeSession activeSession;
        private TimeSession completedSession;

        @BeforeEach
        void setUp() {
                timeSessionRepository.deleteAll();

                activeSession = TimeSession.builder()
                                .appointmentId("appt-123")
                                .employeeId("emp-456")
                                .clockInTime(LocalDateTime.now().minusHours(2))
                                .active(true)
                                .build();
                timeSessionRepository.save(activeSession);

                // Create completed session and manually set it to inactive after save
                completedSession = TimeSession.builder()
                                .appointmentId("appt-789")
                                .employeeId("emp-456")
                                .clockInTime(LocalDateTime.now().minusDays(1))
                                .clockOutTime(LocalDateTime.now().minusDays(1).plusHours(3))
                                .timeLogId("log-999")
                                .build();
                TimeSession saved = timeSessionRepository.save(completedSession);
                saved.setActive(false);
                completedSession = timeSessionRepository.save(saved);
        }

        @Test
        void testFindByAppointmentIdAndActiveTrue() {
                Optional<TimeSession> found = timeSessionRepository
                                .findByAppointmentIdAndActiveTrue("appt-123");

                assertThat(found).isPresent();
                assertThat(found.get().getEmployeeId()).isEqualTo("emp-456");
                assertThat(found.get().isActive()).isTrue();
        }

        @Test
        void testFindByAppointmentIdAndActiveTrue_NotActive() {
                Optional<TimeSession> found = timeSessionRepository
                                .findByAppointmentIdAndActiveTrue("appt-789");

                assertThat(found).isEmpty();
        }

        @Test
        void testFindByAppointmentIdAndEmployeeIdAndActiveTrue() {
                Optional<TimeSession> found = timeSessionRepository
                                .findByAppointmentIdAndEmployeeIdAndActiveTrue("appt-123", "emp-456");

                assertThat(found).isPresent();
                assertThat(found.get().getAppointmentId()).isEqualTo("appt-123");
        }

        @Test
        void testFindByAppointmentIdAndEmployeeIdAndActiveTrue_WrongEmployee() {
                Optional<TimeSession> found = timeSessionRepository
                                .findByAppointmentIdAndEmployeeIdAndActiveTrue("appt-123", "emp-wrong");

                assertThat(found).isEmpty();
        }

        @Test
        void testFindByEmployeeIdAndActiveTrue() {
                // Create another active session for the same employee
                TimeSession session2 = TimeSession.builder()
                                .appointmentId("appt-555")
                                .employeeId("emp-456")
                                .clockInTime(LocalDateTime.now().minusHours(1))
                                .active(true)
                                .build();
                timeSessionRepository.save(session2);

                List<TimeSession> activeSessions = timeSessionRepository
                                .findByEmployeeIdAndActiveTrue("emp-456");

                // Should only have 1 active session (completedSession is inactive)
                assertThat(activeSessions).hasSize(2);
                assertThat(activeSessions).allMatch(TimeSession::isActive);
        }

        @Test
        void testFindByEmployeeIdOrderByClockInTimeDesc() {
                List<TimeSession> sessions = timeSessionRepository
                                .findByEmployeeIdOrderByClockInTimeDesc("emp-456");

                assertThat(sessions).hasSize(2);
                // Most recent should be first
                assertThat(sessions.get(0).getClockInTime())
                                .isAfter(sessions.get(1).getClockInTime());
        }

        @Test
        void testSaveAndRetrieve() {
                TimeSession newSession = TimeSession.builder()
                                .appointmentId("appt-new")
                                .employeeId("emp-new")
                                .clockInTime(LocalDateTime.now())
                                .active(true)
                                .build();

                TimeSession saved = timeSessionRepository.save(newSession);

                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getAppointmentId()).isEqualTo("appt-new");
                assertThat(saved.isActive()).isTrue();
        }

        @Test
        void testClockOut() {
                TimeSession session = timeSessionRepository.findById(activeSession.getId()).orElseThrow();
                session.setClockOutTime(LocalDateTime.now());
                session.setActive(false);
                session.setTimeLogId("log-123");

                TimeSession updated = timeSessionRepository.save(session);

                assertThat(updated.getClockOutTime()).isNotNull();
                assertThat(updated.isActive()).isFalse();
                assertThat(updated.getTimeLogId()).isEqualTo("log-123");
        }
}
