package com.exercise.bookmateserver.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "moderation_reports",
        indexes = {
                @Index(name = "idx_moderation_reports_reporter", columnList = "reporter_user_id"),
                @Index(name = "idx_moderation_reports_target", columnList = "target_type,target_id"),
                @Index(name = "idx_moderation_reports_status", columnList = "status")
        }
)
public class ModerationReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "reporter_user_id", nullable = false)
    private UUID reporterUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    private ModerationTargetType targetType;

    @Column(name = "target_id", nullable = false, length = 120)
    private String targetId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ModerationReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "target_snapshot", columnDefinition = "TEXT")
    private String targetSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModerationReportStatus status = ModerationReportStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected ModerationReportEntity() {
    }

    public ModerationReportEntity(
            UUID reporterUserId,
            ModerationTargetType targetType,
            String targetId,
            UUID targetUserId,
            ModerationReportReason reason,
            String detail,
            String targetSnapshot
    ) {
        this.reporterUserId = reporterUserId;
        this.targetType = targetType;
        this.targetId = normalizeTargetId(targetId);
        this.targetUserId = targetUserId;
        this.reason = reason;
        this.detail = normalizeNullable(detail);
        this.targetSnapshot = normalizeNullable(targetSnapshot);
    }

    public void updateStatus(ModerationReportStatus status) {
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String normalizeTargetId(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return "";
        }

        return targetId.trim();
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    public UUID getId() {
        return id;
    }

    public UUID getReporterUserId() {
        return reporterUserId;
    }

    public ModerationTargetType getTargetType() {
        return targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public UUID getTargetUserId() {
        return targetUserId;
    }

    public ModerationReportReason getReason() {
        return reason;
    }

    public String getDetail() {
        return detail;
    }

    public String getTargetSnapshot() {
        return targetSnapshot;
    }

    public ModerationReportStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
