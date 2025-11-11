package com.techtorque.appointment_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

/**
 * Entity to track active time sessions for appointments
 * This allows employees to clock in/out and automatically track time
 */
@Entity
@Table(name = "time_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeSession {

  @Id
  @GeneratedValue(generator = "uuid")
  @GenericGenerator(name = "uuid", strategy = "uuid2")
  private String id;

  @Column(nullable = false)
  private String appointmentId;

  @Column(nullable = false)
  private String employeeId;

  @Column(nullable = false)
  private LocalDateTime clockInTime;

  private LocalDateTime clockOutTime;

  @Column(nullable = false)
  private boolean active;

  private String timeLogId; // Reference to the time log entry in Time Logging Service

  @PrePersist
  protected void onCreate() {
    if (clockInTime == null) {
      clockInTime = LocalDateTime.now();
    }
    active = true;
  }
}
