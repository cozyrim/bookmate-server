package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_inbox_items")
public class NotificationInboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private UserEntity user;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "data_json", nullable = false, columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    protected NotificationInboxEntity() {
    }

    public NotificationInboxEntity(UserEntity user, PushNotificationPayload payload, String dataJson) {
        this.user = user;
        this.type = notificationType(payload);
        this.title = payload.title();
        this.body = payload.body();
        this.dataJson = dataJson;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() {
        if (read) {
            return;
        }

        read = true;
        readAt = LocalDateTime.now();
    }

    private String notificationType(PushNotificationPayload payload) {
        String type = payload.data() == null ? null : payload.data().get("type");
        if (type == null || type.isBlank()) {
            return "general";
        }

        return type.trim();
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getDataJson() {
        return dataJson;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
