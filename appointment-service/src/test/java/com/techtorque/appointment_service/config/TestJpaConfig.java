package com.techtorque.appointment_service.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EnableJpaAuditing
@EntityScan("com.techtorque.appointment_service.entity")
@EnableJpaRepositories("com.techtorque.appointment_service.repository")
public class TestJpaConfig {
}
