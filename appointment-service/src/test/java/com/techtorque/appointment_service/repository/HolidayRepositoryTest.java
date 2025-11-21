package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.Holiday;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import com.techtorque.appointment_service.config.TestDataSourceConfig;
import org.springframework.context.annotation.Import;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
class HolidayRepositoryTest {

    @Autowired
    private HolidayRepository holidayRepository;

    private Holiday testHoliday;

    @BeforeEach
    void setUp() {
        holidayRepository.deleteAll();

        testHoliday = Holiday.builder()
                .date(LocalDate.of(2025, 12, 25))
                .name("Christmas")
                .description("Christmas Day - Office Closed")
                .build();

        holidayRepository.save(testHoliday);
    }

    @Test
    void testFindByDate() {
        Optional<Holiday> found = holidayRepository.findByDate(LocalDate.of(2025, 12, 25));

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Christmas");
    }

    @Test
    void testFindByDate_NotFound() {
        Optional<Holiday> found = holidayRepository.findByDate(LocalDate.of(2025, 12, 26));

        assertThat(found).isEmpty();
    }

    @Test
    void testExistsByDate() {
        boolean exists = holidayRepository.existsByDate(LocalDate.of(2025, 12, 25));

        assertThat(exists).isTrue();
    }

    @Test
    void testExistsByDate_NotFound() {
        boolean exists = holidayRepository.existsByDate(LocalDate.of(2025, 12, 26));

        assertThat(exists).isFalse();
    }

    @Test
    void testSaveAndRetrieve() {
        Holiday newYearHoliday = Holiday.builder()
                .date(LocalDate.of(2025, 1, 1))
                .name("New Year")
                .description("New Year's Day")
                .build();

        Holiday saved = holidayRepository.save(newYearHoliday);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("New Year");
        assertThat(saved.getDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    }

    @Test
    void testFindAll() {
        Holiday independenceDay = Holiday.builder()
                .date(LocalDate.of(2025, 2, 4))
                .name("Independence Day")
                .description("Sri Lankan Independence Day")
                .build();
        holidayRepository.save(independenceDay);

        assertThat(holidayRepository.findAll()).hasSize(2);
    }
}
