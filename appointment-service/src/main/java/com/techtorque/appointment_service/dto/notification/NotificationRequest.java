package com.techtorque.appointment_service.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String userId;
    private String type;  // INFO, WARNING, ERROR, SUCCESS
    private String message;
    private String details;
    private String relatedEntityId;
    private String relatedEntityType;
}
