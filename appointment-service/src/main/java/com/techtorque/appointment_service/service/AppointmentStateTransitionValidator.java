package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.entity.AppointmentStatus;
import com.techtorque.appointment_service.exception.InvalidStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Validates appointment state transitions and enforces business rules.
 * Implements a strict state machine for appointment lifecycle management.
 */
@Component
public class AppointmentStateTransitionValidator {

  /**
   * Define valid state transitions and the roles that can perform them
   */
  private static final Map<AppointmentStatus, Map<AppointmentStatus, Set<String>>> VALID_TRANSITIONS = new HashMap<>();

  static {
    // PENDING state transitions
    Map<AppointmentStatus, Set<String>> pendingTransitions = new HashMap<>();
    pendingTransitions.put(AppointmentStatus.CONFIRMED, Set.of("ADMIN", "SUPER_ADMIN")); // Admin assigns employee
    pendingTransitions.put(AppointmentStatus.CANCELLED, Set.of("CUSTOMER", "ADMIN", "SUPER_ADMIN")); // Customer or admin cancels
    VALID_TRANSITIONS.put(AppointmentStatus.PENDING, pendingTransitions);

    // CONFIRMED state transitions
    Map<AppointmentStatus, Set<String>> confirmedTransitions = new HashMap<>();
    confirmedTransitions.put(AppointmentStatus.IN_PROGRESS, Set.of("EMPLOYEE", "ADMIN", "SUPER_ADMIN")); // Employee starts work
    confirmedTransitions.put(AppointmentStatus.NO_SHOW, Set.of("ADMIN", "SUPER_ADMIN")); // Admin marks as no-show
    confirmedTransitions.put(AppointmentStatus.CANCELLED, Set.of("ADMIN", "SUPER_ADMIN")); // Admin cancels
    VALID_TRANSITIONS.put(AppointmentStatus.CONFIRMED, confirmedTransitions);

    // IN_PROGRESS state transitions
    Map<AppointmentStatus, Set<String>> inProgressTransitions = new HashMap<>();
    inProgressTransitions.put(AppointmentStatus.COMPLETED, Set.of("EMPLOYEE", "ADMIN", "SUPER_ADMIN")); // Employee marks complete
    inProgressTransitions.put(AppointmentStatus.CANCELLED, Set.of("ADMIN", "SUPER_ADMIN")); // Admin cancels
    VALID_TRANSITIONS.put(AppointmentStatus.IN_PROGRESS, inProgressTransitions);

    // COMPLETED state transitions
    Map<AppointmentStatus, Set<String>> completedTransitions = new HashMap<>();
    completedTransitions.put(AppointmentStatus.CUSTOMER_CONFIRMED, Set.of("CUSTOMER")); // Customer confirms completion
    VALID_TRANSITIONS.put(AppointmentStatus.COMPLETED, completedTransitions);

    // Terminal states - no transitions allowed
    VALID_TRANSITIONS.put(AppointmentStatus.CUSTOMER_CONFIRMED, new HashMap<>()); // Terminal
    VALID_TRANSITIONS.put(AppointmentStatus.CANCELLED, new HashMap<>()); // Terminal
    VALID_TRANSITIONS.put(AppointmentStatus.NO_SHOW, new HashMap<>()); // Terminal
  }

  /**
   * Validates if a state transition is allowed for a given user role
   *
   * @param currentStatus Current appointment status
   * @param newStatus Desired new status
   * @param userRole Role of the user attempting the transition
   * @throws InvalidStatusTransitionException if transition is not allowed
   */
  public void validateTransition(AppointmentStatus currentStatus, AppointmentStatus newStatus, String userRole) {
    // Check if current status exists in valid transitions
    if (!VALID_TRANSITIONS.containsKey(currentStatus)) {
      throw new InvalidStatusTransitionException(
          currentStatus, newStatus,
          "Current status '" + currentStatus + "' is not recognized in state machine");
    }

    // Get allowed transitions from current status
    Map<AppointmentStatus, Set<String>> allowedTransitions = VALID_TRANSITIONS.get(currentStatus);

    // Check if the new status is in the allowed transitions
    if (!allowedTransitions.containsKey(newStatus)) {
      throw new InvalidStatusTransitionException(
          currentStatus, newStatus,
          "Transition from '" + currentStatus + "' to '" + newStatus + "' is not allowed");
    }

    // Check if the user role is authorized for this transition
    Set<String> allowedRoles = allowedTransitions.get(newStatus);
    if (!allowedRoles.contains(userRole)) {
      throw new InvalidStatusTransitionException(
          currentStatus, newStatus,
          "User role '" + userRole + "' is not authorized to transition from '" + currentStatus + "' to '" + newStatus + "'. " +
              "Allowed roles: " + String.join(", ", allowedRoles));
    }
  }

  /**
   * Gets all valid target states from a given current state
   *
   * @param currentStatus Current appointment status
   * @return Map of valid target states and allowed roles
   */
  public Map<AppointmentStatus, Set<String>> getValidTransitions(AppointmentStatus currentStatus) {
    return VALID_TRANSITIONS.getOrDefault(currentStatus, new HashMap<>());
  }

  /**
   * Checks if a status is a terminal state (no transitions allowed)
   *
   * @param status Appointment status
   * @return true if status is terminal
   */
  public boolean isTerminalState(AppointmentStatus status) {
    return getValidTransitions(status).isEmpty();
  }

  /**
   * Gets allowed roles for a specific transition
   *
   * @param currentStatus Current status
   * @param newStatus Target status
   * @return Set of allowed roles, empty set if transition not allowed
   */
  public Set<String> getAllowedRoles(AppointmentStatus currentStatus, AppointmentStatus newStatus) {
    return VALID_TRANSITIONS
        .getOrDefault(currentStatus, new HashMap<>())
        .getOrDefault(newStatus, new HashSet<>());
  }
}

