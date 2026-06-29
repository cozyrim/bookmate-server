package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "notification_device_tokens",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_device_tokens_fcm_token", columnNames = "fcm_token")
        },
        indexes = {
                @Index(name = "idx_notification_device_tokens_user_enabled", columnList = "user_id, enabled")
        }
)
public class NotificationDeviceTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "fcm_token", nullable = false, columnDefinition = "TEXT")
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPlatform platform;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "app_version", length = 64)
    private String appVersion;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    protected NotificationDeviceTokenEntity() {
    }

    public NotificationDeviceTokenEntity(
            UserEntity user,
            String fcmToken,
            NotificationPlatform platform,
            String deviceId,
            String appVersion
    ) {
        this.user = user;
        this.fcmToken = normalizeRequired(fcmToken);
        this.platform = platform;
        this.deviceId = normalizeNullable(deviceId);
        this.appVersion = normalizeNullable(appVersion);
        this.enabled = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void refresh(UserEntity user, NotificationPlatform platform, String deviceId, String appVersion) {
        this.user = user;
        this.platform = platform;
        this.deviceId = normalizeNullable(deviceId);
        this.appVersion = normalizeNullable(appVersion);
        this.enabled = true;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void disable() {
        this.enabled = false;
        this.lastSeenAt = LocalDateTime.now();
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lastSeenAt == null) {
            this.lastSeenAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("FCM token is required.");
        }

        return value.trim();
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

    public UserEntity getUser() {
        return user;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public NotificationPlatform getPlatform() {
        return platform;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }
}
