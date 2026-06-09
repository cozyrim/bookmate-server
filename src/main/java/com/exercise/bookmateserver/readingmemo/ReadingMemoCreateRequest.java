package com.exercise.bookmateserver.readingmemo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ReadingMemoCreateRequest(
        @NotNull UUID bookId,
        @NotBlank String date,
        Integer page,
        @NotBlank String text
) {
}
