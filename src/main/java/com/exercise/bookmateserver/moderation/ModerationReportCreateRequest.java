package com.exercise.bookmateserver.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record ModerationReportCreateRequest(
        @NotNull ModerationTargetType targetType,
        @NotBlank @Size(max = 120) String targetId,
        UUID targetUserId,
        @NotNull ModerationReportReason reason,
        @Size(max = 1000) String detail,
        @Size(max = 20) Map<String, String> targetSnapshot
) {
}
