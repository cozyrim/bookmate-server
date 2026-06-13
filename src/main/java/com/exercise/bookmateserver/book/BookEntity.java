package com.exercise.bookmateserver.book;

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
@Table(name = "books")
public class BookEntity {

    private static final String DEFAULT_CATEGORY = "카테고리 선택";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String imageName;

    private String isbn;

    @Column(nullable = false)
    private Double progress;

    private Integer totalPages;

    private Integer currentPage;

    @Column(nullable = false, columnDefinition = "varchar(255) default '카테고리 선택'")
    private String category = DEFAULT_CATEGORY;

    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String review;

    private String readingStatus;

    private String startDate;

    private String endDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected BookEntity() {
    }

    public BookEntity(
            UUID userId,
            String title,
            String author,
            String imageName,
            String isbn,
            Double progress,
            String category,
            Integer totalPages,
            Integer currentPage
    ) {
        this.userId = userId;
        this.title = title;
        this.author = author;
        this.imageName = imageName;
        this.isbn = BookIsbnNormalizer.normalize(isbn);
        this.category = normalizeCategory(category);
        updateReadingProgress(progress, totalPages, currentPage);
    }

    public void update(
            String title,
            String author,
            String imageName,
            String isbn,
            Double progress,
            String category,
            Integer totalPages,
            Integer currentPage,
            Integer rating,
            String review,
            String readingStatus,
            String startDate,
            String endDate
    ) {
        this.title = title;
        this.author = author;
        this.imageName = imageName;
        if (isbn != null) {
            this.isbn = BookIsbnNormalizer.normalize(isbn);
        }
        this.category = normalizeCategory(category);
        updateReadingProgress(progress, totalPages, currentPage);
        this.rating = normalizeRating(rating);
        this.review = review;
        this.readingStatus = readingStatus;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @PrePersist
    void onCreate() {
        this.category = normalizeCategory(category);
        this.createdAt = LocalDateTime.now();
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return DEFAULT_CATEGORY;
        }

        return category.trim();
    }

    private Integer normalizeRating(Integer rating) {
        if (rating == null) return null;
        if (rating < 1) return 1;
        if (rating > 5) return 5;
        return rating;
    }

    private void updateReadingProgress(Double progress, Integer totalPages, Integer currentPage) {
        this.totalPages = normalizePage(totalPages);
        this.currentPage = normalizePage(currentPage);

        if (this.totalPages != null && this.totalPages > 0 && this.currentPage != null) {
            this.currentPage = Math.min(this.currentPage, this.totalPages);
            this.progress = (double) this.currentPage / this.totalPages;
            return;
        }

        this.progress = normalizeProgress(progress);
    }

    private Integer normalizePage(Integer page) {
        if (page == null || page < 0) {
            return null;
        }

        return page;
    }

    private Double normalizeProgress(Double progress) {
        if (progress == null || progress.isNaN() || progress.isInfinite()) {
            return 0.0;
        }

        if (progress < 0.0) {
            return 0.0;
        }

        if (progress > 1.0) {
            return 1.0;
        }

        return progress;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getImageName() {
        return imageName;
    }

    public String getIsbn() {
        return isbn;
    }

    public Double getProgress() {
        return progress;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public String getCategory() {
        return normalizeCategory(category);
    }

    public Integer getRating() {
        return rating;
    }

    public String getReview() {
        return review;
    }

    public String getReadingStatus() {
        return readingStatus;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
