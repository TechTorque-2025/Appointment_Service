package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.BusinessHours;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalTime;
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
class BusinessHoursRepositoryTest {

    @Autowired
    private BusinessHoursRepository businessHoursRepository;

    private BusinessHours mondayHours;
    private BusinessHours sundayHours;

    @BeforeEach
    void setUp() {
        businessHoursRepository.deleteAll();

        mondayHours = BusinessHours.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .breakStartTime(LocalTime.of(13, 0))
                .breakEndTime(LocalTime.of(14, 0))
                .isOpen(true)
                .build();

        sundayHours = BusinessHours.builder()
                .dayOfWeek(DayOfWeek.SUNDAY)
                .openTime(LocalTime.of(9, 0))
                .closeTime(LocalTime.of(18, 0))
                .isOpen(false)
                .build();

        businessHoursRepository.save(mondayHours);
        businessHoursRepository.save(sundayHours);
    }

    @Test
    void testFindByDayOfWeek() {
        Optional<BusinessHours> found = businessHoursRepository
                .findByDayOfWeek(DayOfWeek.MONDAY);

        assertThat(found).isPresent();
        assertThat(found.get().getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(found.get().getOpenTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(found.get().getCloseTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void testFindByDayOfWeek_Sunday() {
        Optional<BusinessHours> found = businessHoursRepository
                .findByDayOfWeek(DayOfWeek.SUNDAY);

        assertThat(found).isPresent();
        assertThat(found.get().getIsOpen()).isFalse();
    }

    @Test
    void testFindByDayOfWeek_NotFound() {
        Optional<BusinessHours> found = businessHoursRepository
                .findByDayOfWeek(DayOfWeek.SATURDAY);

        assertThat(found).isEmpty();
    }

    @Test
    void testSaveAndRetrieve() {
        BusinessHours wednesdayHours = BusinessHours.builder()
                .dayOfWeek(DayOfWeek.WEDNESDAY)
                .openTime(LocalTime.of(8, 30))
                .closeTime(LocalTime.of(17, 30))
                .breakStartTime(LocalTime.of(12, 30))
                .breakEndTime(LocalTime.of(13, 30))
                .isOpen(true)
                .build();

        BusinessHours saved = businessHoursRepository.save(wednesdayHours);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getDayOfWeek()).isEqualTo(DayOfWeek.WEDNESDAY);
        assertThat(saved.getOpenTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(saved.getCloseTime()).isEqualTo(LocalTime.of(17, 30));
    }

    @Test
    void testFindAll() {
        assertThat(businessHoursRepository.findAll()).hasSize(2);
    }
}
