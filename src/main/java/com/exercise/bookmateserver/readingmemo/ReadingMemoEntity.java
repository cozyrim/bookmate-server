package com.exercise.bookmateserver.readingmemo;

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
@Table(name = "reading_memos")
public class ReadingMemoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private String date; // e.g., "2026-01-16"

    private Integer page;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ReadingMemoEntity() {
    }

    public ReadingMemoEntity(UUID userId, UUID bookId, String date, Integer page, String text) {
        this.userId = userId;
        this.bookId = bookId;
        this.date = date;
        this.page = page;
        this.text = text;
    }

    public void update(String date, Integer page, String text) {
        this.date = date;
        this.page = page;
        this.text = text;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getBookId() { return bookId; }
    public String getDate() { return date; }
    public Integer getPage() { return page; }
    public String getText() { return text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
