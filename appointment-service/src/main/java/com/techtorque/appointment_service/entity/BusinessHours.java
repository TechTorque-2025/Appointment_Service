package com.techtorque.appointment_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "business_hours")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHours {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  // store DayOfWeek as STRING (e.g. "MONDAY") to match production database column type
  // using STRING avoids numeric/ordinal mismatches across different DB schemas
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DayOfWeek dayOfWeek;

  @Column(nullable = false)
  private LocalTime openTime;

  @Column(nullable = false)
  private LocalTime closeTime;

  private LocalTime breakStartTime;

  private LocalTime breakEndTime;

  @Column(nullable = false)
  @Builder.Default
  private Boolean isOpen = true;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private LocalDateTime updatedAt;
}
