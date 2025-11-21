package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.ServiceBay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.techtorque.appointment_service.config.TestDataSourceConfig;
import org.springframework.context.annotation.Import;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
class ServiceBayRepositoryTest {

    @Autowired
    private ServiceBayRepository serviceBayRepository;

    private ServiceBay activeBay;
    private ServiceBay inactiveBay;

    @BeforeEach
    void setUp() {
        serviceBayRepository.deleteAll();

        activeBay = ServiceBay.builder()
                .bayNumber("BAY-01")
                .name("Bay 1 - General Service")
                .description("General maintenance and repair bay")
                .capacity(1)
                .active(true)
                .build();

        inactiveBay = ServiceBay.builder()
                .bayNumber("BAY-03")
                .name("Bay 3 - Under Maintenance")
                .description("Currently under renovation")
                .capacity(1)
                .active(false)
                .build();

        serviceBayRepository.save(activeBay);
        serviceBayRepository.save(inactiveBay);
    }

    @Test
    void testFindByActiveTrue() {
        List<ServiceBay> activeBays = serviceBayRepository.findByActiveTrue();

        assertThat(activeBays).hasSize(1);
        assertThat(activeBays.get(0).getBayNumber()).isEqualTo("BAY-01");
        assertThat(activeBays.get(0).getActive()).isTrue();
    }

    @Test
    void testFindByActiveTrueOrderByBayNumberAsc() {
        // Create additional active bay
        ServiceBay bay2 = ServiceBay.builder()
                .bayNumber("BAY-02")
                .name("Bay 2 - Quick Service")
                .description("Fast oil change and tire rotation")
                .capacity(1)
                .active(true)
                .build();
        serviceBayRepository.save(bay2);

        List<ServiceBay> activeBays = serviceBayRepository.findByActiveTrueOrderByBayNumberAsc();

        assertThat(activeBays).hasSize(2);
        assertThat(activeBays.get(0).getBayNumber()).isEqualTo("BAY-01");
        assertThat(activeBays.get(1).getBayNumber()).isEqualTo("BAY-02");
    }

    @Test
    void testSaveAndRetrieve() {
        ServiceBay newBay = ServiceBay.builder()
                .bayNumber("BAY-04")
                .name("Bay 4 - Heavy Duty")
                .description("For trucks and heavy vehicles")
                .capacity(1)
                .active(true)
                .build();

        ServiceBay saved = serviceBayRepository.save(newBay);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getBayNumber()).isEqualTo("BAY-04");
        assertThat(saved.getName()).isEqualTo("Bay 4 - Heavy Duty");
        assertThat(saved.getActive()).isTrue();
    }

    @Test
    void testFindAll() {
        List<ServiceBay> allBays = serviceBayRepository.findAll();

        assertThat(allBays).hasSize(2);
    }

    @Test
    void testCapacityDefault() {
        assertThat(activeBay.getCapacity()).isEqualTo(1);
    }
}
