package com.exercise.bookmateserver.social.guestbook;

import com.exercise.bookmateserver.user.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "guestbook_messages")
public class GuestbookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private UserEntity targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_user_id", nullable = false)
    private UserEntity writerUser;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected GuestbookEntity() {
    }

    public GuestbookEntity(UserEntity targetUser, UserEntity writerUser, String content) {
        this.targetUser = targetUser;
        this.writerUser = writerUser;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UserEntity getTargetUser() {
        return targetUser;
    }

    public UserEntity getWriterUser() {
        return writerUser;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
