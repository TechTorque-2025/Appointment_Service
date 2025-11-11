package com.techtorque.appointment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "appointments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, updatable = false)
  private String customerId; // Foreign key to the user

  @Column(nullable = false)
  private String vehicleId; // Foreign key to the vehicle

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "appointment_assigned_employees", joinColumns = @JoinColumn(name = "appointment_id"))
  @Column(name = "employee_id")
  @Builder.Default
  private Set<String> assignedEmployeeIds = new HashSet<>(); // Multiple employees can be assigned

  private String assignedBayId; // Foreign key to ServiceBay

  @Column(unique = true)
  private String confirmationNumber; // e.g., "APT-2025-001234"

  @Column(nullable = false)
  private String serviceType;

  @Column(nullable = false)
  private LocalDateTime requestedDateTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppointmentStatus status;

  @Lob // For potentially long instructions
  private String specialInstructions;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;

  // Vehicle arrival tracking
  private LocalDateTime vehicleArrivedAt; // When employee confirmed vehicle arrival
  private String vehicleAcceptedByEmployeeId; // Which employee accepted the vehicle
}