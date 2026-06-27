package com.exercise.bookmateserver.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDeviceTokenResponse(
        UUID id,
        NotificationPlatform platform,
        String deviceId,
        String appVersion,
        boolean enabled,
        LocalDateTime lastSeenAt
) {
    public static NotificationDeviceTokenResponse from(NotificationDeviceTokenEntity token) {
        return new NotificationDeviceTokenResponse(
                token.getId(),
                token.getPlatform(),
                token.getDeviceId(),
                token.getAppVersion(),
                token.isEnabled(),
                token.getLastSeenAt()
        );
    }
}
