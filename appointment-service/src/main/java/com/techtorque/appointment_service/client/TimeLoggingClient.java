package com.techtorque.appointment_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with Time Logging Service
 * Used to automatically create time log entries when employees clock in/out
 */
@Component
@Slf4j
public class TimeLoggingClient {

    private final RestTemplate restTemplate;
    private final String timeLoggingServiceUrl;

    public TimeLoggingClient(RestTemplate restTemplate,
                             @Value("${services.time-logging.url:http://localhost:8085}") String timeLoggingServiceUrl) {
        this.restTemplate = restTemplate;
        this.timeLoggingServiceUrl = timeLoggingServiceUrl;
    }

    /**
     * Create a time log entry when employee clocks in
     * This starts tracking time for the appointment/service
     *
     * @param employeeId The employee who is clocking in
     * @param appointmentId The appointment ID (used as serviceId in time logging)
     * @param description Description of the work
     * @param hours Initial hours (will be 0 when clocking in, updated on clock out)
     * @return The time log ID from the Time Logging Service
     */
    public String createTimeLog(String employeeId, String appointmentId, String description, double hours) {
        try {
            log.info("Creating time log for employee {} on appointment {}", employeeId, appointmentId);

            Map<String, Object> request = new HashMap<>();
            request.put("serviceId", appointmentId); // Using appointmentId as serviceId
            request.put("date", LocalDate.now().toString());
            request.put("hours", hours);
            request.put("description", description);
            request.put("workType", "APPOINTMENT");

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                timeLoggingServiceUrl + "/time-logs",
                entity,
                Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("id")) {
                String timeLogId = (String) response.getBody().get("id");
                log.debug("Time log created successfully with ID: {}", timeLogId);
                return timeLogId;
            }

            log.warn("Time log created but no ID returned");
            return null;

        } catch (Exception e) {
            log.error("Failed to create time log for employee {}: {}", employeeId, e.getMessage());
            return null;
        }
    }

    /**
     * Update a time log entry when employee clocks out
     *
     * @param timeLogId The time log ID to update
     * @param hours The total hours worked
     * @param description Updated description
     */
    public void updateTimeLog(String timeLogId, double hours, String description) {
        try {
            log.info("Updating time log {} with {} hours", timeLogId, hours);

            Map<String, Object> request = new HashMap<>();
            request.put("hours", hours);
            if (description != null) {
                request.put("description", description);
            }

            HttpHeaders headers = createAuthHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(
                timeLoggingServiceUrl + "/time-logs/" + timeLogId,
                HttpMethod.PUT,
                entity,
                Object.class
            );

            log.debug("Time log updated successfully");

        } catch (Exception e) {
            log.error("Failed to update time log {}: {}", timeLogId, e.getMessage());
        }
    }

    /**
     * Create HTTP headers with JWT token for authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String token = jwtAuth.getToken().getTokenValue();
                headers.set("Authorization", "Bearer " + token);
            }
        } catch (Exception e) {
            log.warn("Failed to add authentication token to time logging request: {}", e.getMessage());
        }
        
        return headers;
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use createTimeLog instead
     */
    @Deprecated
    public void startTimeLog(String employeeId, String appointmentId, String description) {
        createTimeLog(employeeId, appointmentId, description, 0.0);
    }
}
