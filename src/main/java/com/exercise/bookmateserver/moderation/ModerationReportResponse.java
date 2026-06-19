package com.exercise.bookmateserver.moderation;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModerationReportResponse(
        UUID id,
        UUID reporterUserId,
        ModerationTargetType targetType,
        String targetId,
        UUID targetUserId,
        ModerationReportReason reason,
        String detail,
        ModerationReportStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ModerationReportResponse from(ModerationReportEntity report) {
        return new ModerationReportResponse(
                report.getId(),
                report.getReporterUserId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getTargetUserId(),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
