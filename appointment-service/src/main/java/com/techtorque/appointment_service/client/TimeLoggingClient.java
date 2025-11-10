package com.techtorque.appointment_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Client for communicating with Time Logging Service
 * Used to automatically create time log entries when employees start work
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
     * Create a time log entry when employee accepts vehicle arrival
     * This starts tracking time for the appointment/service
     *
     * @param employeeId The employee who accepted the vehicle
     * @param appointmentId The appointment ID (used as serviceId in time logging)
     * @param description Description of the work
     */
    public void startTimeLog(String employeeId, String appointmentId, String description) {
        try {
            log.info("Creating time log for employee {} on appointment {}", employeeId, appointmentId);

            Map<String, Object> request = new HashMap<>();
            request.put("employeeId", employeeId);
            request.put("serviceId", appointmentId); // Using appointmentId as serviceId
            request.put("date", LocalDate.now().toString());
            request.put("hours", 0.0); // Will be calculated when work completes
            request.put("description", description);
            request.put("workType", "APPOINTMENT");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(
                timeLoggingServiceUrl + "/api/v1/time-logs",
                entity,
                Object.class
            );

            log.debug("Time log created successfully");

        } catch (Exception e) {
            // Log error but don't throw - time logging failure shouldn't break appointment operations
            log.error("Failed to create time log for employee {}: {}", employeeId, e.getMessage());
        }
    }
}
