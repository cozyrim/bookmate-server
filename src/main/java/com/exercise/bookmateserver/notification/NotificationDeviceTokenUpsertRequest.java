package com.exercise.bookmateserver.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NotificationDeviceTokenUpsertRequest(
        @NotBlank
        @Size(max = 4096)
        String token,

        @NotNull
        NotificationPlatform platform,

        @Size(max = 128)
        String deviceId,

        @Size(max = 64)
        String appVersion
) {
}
