package com.exercise.bookmateserver.readingmemo;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReadingMemoResponse(
        UUID id,
        UUID bookId,
        String date,
        Integer page,
        String text,
        LocalDateTime createdAt
) {
    public static ReadingMemoResponse from(ReadingMemoEntity memo) {
        return new ReadingMemoResponse(
                memo.getId(),
                memo.getBookId(),
                memo.getDate(),
                memo.getPage(),
                memo.getText(),
                memo.getCreatedAt()
        );
    }
}
