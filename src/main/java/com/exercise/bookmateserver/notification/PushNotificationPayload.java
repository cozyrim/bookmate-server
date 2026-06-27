package com.exercise.bookmateserver.notification;

import java.util.Map;

public record PushNotificationPayload(
        String title,
        String body,
        Map<String, String> data
) {
}
