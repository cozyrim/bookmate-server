package com.exercise.bookmateserver.notification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationInboxResponse(
        UUID id,
        String type,
        String title,
        String body,
        Map<String, String> data,
        boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationInboxResponse from(NotificationInboxEntity entity, Map<String, String> data) {
        return new NotificationInboxResponse(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                data,
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }
}
