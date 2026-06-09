package com.exercise.bookmateserver.word;

import java.time.LocalDateTime;
import java.util.UUID;

public record WordResponse(
        UUID id,
        UUID bookId,
        String text,
        String meaning,
        String partOfSpeech,
        String exampleSentence,
        String targetCode,
        LocalDateTime createdAt
) {

    public static WordResponse from(WordEntity word) {
        return new WordResponse(
                word.getId(),
                word.getBookId(),
                word.getText(),
                word.getMeaning(),
                word.getPartOfSpeech(),
                word.getExampleSentence(),
                word.getTargetCode(),
                word.getCreatedAt()
        );
    }
}
