package com.exercise.bookmateserver.quote;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuoteResponse(
        UUID id,
        UUID bookId,
        String text,
        Integer page,
        String memo,
        LocalDateTime createdAt
) {

    public static QuoteResponse from(QuoteEntity quote) {
        return new QuoteResponse(
                quote.getId(),
                quote.getBookId(),
                quote.getText(),
                quote.getPage(),
                quote.getMemo(),
                quote.getCreatedAt()
        );
    }
}
