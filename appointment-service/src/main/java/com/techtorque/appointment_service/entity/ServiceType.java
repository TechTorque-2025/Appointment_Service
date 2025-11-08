package com.techtorque.appointment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_types")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceType {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private String category; // e.g., "Maintenance", "Repair", "Modification"

  @Column(nullable = false)
  private BigDecimal basePriceLKR;

  @Column(nullable = false)
  private Integer estimatedDurationMinutes;

  @Lob
  private String description;

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
