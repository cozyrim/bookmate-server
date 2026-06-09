package com.exercise.bookmateserver.book;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String author,
        String imageName,
        String category,
        Double progress,
        Integer totalPages,
        Integer currentPage,
        Integer rating,
        String review,
        String readingStatus,
        String startDate,
        String endDate,
        LocalDateTime createdAt
) {

    public static BookResponse from(BookEntity book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getImageName(),
                book.getCategory(),
                book.getProgress(),
                book.getTotalPages(),
                book.getCurrentPage(),
                book.getRating(),
                book.getReview(),
                book.getReadingStatus(),
                book.getStartDate(),
                book.getEndDate(),
                book.getCreatedAt()
        );
    }
}

