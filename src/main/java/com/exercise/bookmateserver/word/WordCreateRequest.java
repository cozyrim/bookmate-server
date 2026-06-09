package com.exercise.bookmateserver.word;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record WordCreateRequest(
        @NotNull UUID bookId,
        @NotBlank String text,
        @NotBlank String meaning,
        String partOfSpeech,
        String exampleSentence,
        String targetCode
) {
}
