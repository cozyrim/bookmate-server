package com.exercise.bookmateserver.moderation;

public record ModerationReportEnvelope(
        ModerationReportResponse report,
        int reportCount,
        boolean shouldHideTarget
) {
}
