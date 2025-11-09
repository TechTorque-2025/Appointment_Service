package com.techtorque.appointment_service.service;

import com.techtorque.appointment_service.dto.notification.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Client service for sending notifications to Notification Service
 * Communicates via REST API to create user notifications
 */
@Service
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final String notificationServiceUrl;

    public NotificationClient(RestTemplate restTemplate,
                              @Value("${notification.service.url:http://localhost:8088}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.notificationServiceUrl = notificationServiceUrl;
    }

    /**
     * Send notification to user asynchronously
     * Non-blocking - failures won't affect appointment operations
     */
    public void sendNotification(NotificationRequest request) {
        try {
            log.info("Sending notification to user: {} - {}", request.getUserId(), request.getMessage());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(
                notificationServiceUrl + "/api/v1/notifications/create",
                entity,
                Void.class
            );

            log.debug("Notification sent successfully");

        } catch (Exception e) {
            // Log error but don't throw - notification failure shouldn't break appointment operations
            log.error("Failed to send notification to user {}: {}", request.getUserId(), e.getMessage());
        }
    }

    /**
     * Helper method to create and send appointment notification
     */
    public void sendAppointmentNotification(String userId, String type, String message,
                                             String details, String appointmentId) {
        NotificationRequest request = NotificationRequest.builder()
            .userId(userId)
            .type(type)
            .message(message)
            .details(details)
            .relatedEntityId(appointmentId)
            .relatedEntityType("APPOINTMENT")
            .build();

        sendNotification(request);
    }
}
