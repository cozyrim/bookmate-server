package com.exercise.bookmateserver.word;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

// 디비 테이블과 연결되는 객체 JPA가 words 테이블 만듦
@Entity
@Table(name = "words")
public class WordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private UUID bookId;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meaning;

    private String partOfSpeech;

    @Column(columnDefinition = "TEXT")
    private String exampleSentence;

    private String targetCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected WordEntity() {
    }

    public WordEntity(UUID userId, UUID bookId, String text, String meaning, String partOfSpeech, String exampleSentence, String targetCode) {
        this.userId = userId;
        this.bookId = bookId;
        this.text = text;
        this.meaning = meaning;
        this.partOfSpeech = partOfSpeech;
        this.exampleSentence = exampleSentence;
        this.targetCode = targetCode;
    }

    public void update(UUID bookId, String text, String meaning, String partOfSpeech, String exampleSentence, String targetCode) {
        this.bookId = bookId;
        this.text = text;
        this.meaning = meaning;
        this.partOfSpeech = partOfSpeech;
        this.exampleSentence = exampleSentence;
        this.targetCode = targetCode;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getBookId() {
        return bookId;
    }

    public String getText() {
        return text;
    }

    public String getMeaning() {
        return meaning;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public String getExampleSentence() {
        return exampleSentence;
    }

    public String getTargetCode() {
        return targetCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
