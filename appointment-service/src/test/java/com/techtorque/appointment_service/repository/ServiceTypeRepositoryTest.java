package com.techtorque.appointment_service.repository;

import com.techtorque.appointment_service.entity.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import com.techtorque.appointment_service.config.TestDataSourceConfig;
import org.springframework.context.annotation.Import;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(TestDataSourceConfig.class)
class ServiceTypeRepositoryTest {

        @Autowired
        private ServiceTypeRepository serviceTypeRepository;

        private ServiceType activeService;
        private ServiceType inactiveService;

        @BeforeEach
        void setUp() {
                serviceTypeRepository.deleteAll();

                activeService = ServiceType.builder()
                                .name("Oil Change")
                                .category("Maintenance")
                                .basePriceLKR(BigDecimal.valueOf(5000.00))
                                .estimatedDurationMinutes(60)
                                .description("Regular oil change service")
                                .active(true)
                                .build();

                inactiveService = ServiceType.builder()
                                .name("Engine Overhaul")
                                .category("Repair")
                                .basePriceLKR(BigDecimal.valueOf(50000.00))
                                .estimatedDurationMinutes(480)
                                .description("Complete engine overhaul")
                                .active(false)
                                .build();

                serviceTypeRepository.save(activeService);
                serviceTypeRepository.save(inactiveService);
        }

        @Test
        void testFindByActiveTrue() {
                List<ServiceType> activeServices = serviceTypeRepository.findByActiveTrue();

                assertThat(activeServices).hasSize(1);
                assertThat(activeServices.get(0).getName()).isEqualTo("Oil Change");
                assertThat(activeServices.get(0).getActive()).isTrue();
        }

        @Test
        void testFindByNameAndActiveTrue() {
                Optional<ServiceType> found = serviceTypeRepository
                                .findByNameAndActiveTrue("Oil Change");

                assertThat(found).isPresent();
                assertThat(found.get().getName()).isEqualTo("Oil Change");
        }

        @Test
        void testFindByNameAndActiveTrue_Inactive() {
                Optional<ServiceType> found = serviceTypeRepository
                                .findByNameAndActiveTrue("Engine Overhaul");

                assertThat(found).isEmpty();
        }

        @Test
        void testFindByCategoryAndActiveTrue() {
                // Create another active maintenance service
                ServiceType tireRotation = ServiceType.builder()
                                .name("Tire Rotation")
                                .category("Maintenance")
                                .basePriceLKR(BigDecimal.valueOf(3000.00))
                                .estimatedDurationMinutes(30)
                                .active(true)
                                .build();
                serviceTypeRepository.save(tireRotation);

                List<ServiceType> maintenanceServices = serviceTypeRepository
                                .findByCategoryAndActiveTrue("Maintenance");

                assertThat(maintenanceServices).hasSize(2);
                assertThat(maintenanceServices).extracting(ServiceType::getCategory)
                                .containsOnly("Maintenance");
                assertThat(maintenanceServices).extracting(ServiceType::getActive)
                                .containsOnly(true);
        }

        @Test
        void testSaveAndRetrieve() {
                ServiceType newService = ServiceType.builder()
                                .name("Brake Service")
                                .category("Repair")
                                .basePriceLKR(BigDecimal.valueOf(15000.00))
                                .estimatedDurationMinutes(120)
                                .description("Complete brake inspection and replacement")
                                .active(true)
                                .build();

                ServiceType saved = serviceTypeRepository.save(newService);

                assertThat(saved.getId()).isNotNull();
                assertThat(saved.getName()).isEqualTo("Brake Service");
                assertThat(saved.getCategory()).isEqualTo("Repair");
                assertThat(saved.getActive()).isTrue();
        }

        @Test
        void testFindAll() {
                List<ServiceType> allServices = serviceTypeRepository.findAll();

                assertThat(allServices).hasSize(2);
        }
}
