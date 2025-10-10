package com.techtorque.appointment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class AppointmentServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AppointmentServiceApplication.class, args);
  }
}