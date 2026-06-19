package com.exercise.bookmateserver.moderation;

import jakarta.validation.constraints.NotNull;

public record ModerationReportStatusUpdateRequest(
        @NotNull ModerationReportStatus status
) {
}
