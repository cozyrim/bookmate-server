package com.exercise.bookmateserver.quote;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "quotes")
public class QuoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private UUID bookId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    private Integer page;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected QuoteEntity() {
    }

    public QuoteEntity(UUID userId, UUID bookId, String text, Integer page, String memo) {
        this.userId = userId;
        this.bookId = bookId;
        this.text = text;
        this.page = page;
        this.memo = memo;
    }

    public void update(UUID bookId, String text, Integer page, String memo) {
        this.bookId = bookId;
        this.text = text;
        this.page = page;
        this.memo = memo;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getBookId() { return bookId; }
    public String getText() { return text; }
    public Integer getPage() { return page; }
    public String getMemo() { return memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
