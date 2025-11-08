package com.techtorque.appointment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_bays")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBay {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  private String bayNumber; // e.g., "BAY-01", "BAY-02"

  @Column(nullable = false)
  private String name; // e.g., "Bay 1 - General Service"

  @Lob
  private String description;

  @Column(nullable = false)
  @Builder.Default
  private Integer capacity = 1; // Number of concurrent appointments (usually 1)

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
