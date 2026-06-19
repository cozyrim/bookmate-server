package com.exercise.bookmateserver.moderation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_blocks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_blocks_pair", columnNames = {"blocker_user_id", "blocked_user_id"})
        },
        indexes = {
                @Index(name = "idx_user_blocks_blocker", columnList = "blocker_user_id"),
                @Index(name = "idx_user_blocks_blocked", columnList = "blocked_user_id")
        }
)
public class UserBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blocker_user_id", nullable = false)
    private UUID blockerUserId;

    @Column(name = "blocked_user_id", nullable = false)
    private UUID blockedUserId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected UserBlockEntity() {
    }

    public UserBlockEntity(UUID blockerUserId, UUID blockedUserId) {
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getBlockerUserId() {
        return blockerUserId;
    }

    public UUID getBlockedUserId() {
        return blockedUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
