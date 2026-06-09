package com.exercise.bookmateserver.readingmemo;

import jakarta.validation.constraints.NotBlank;

public record ReadingMemoUpdateRequest(
        @NotBlank String date,
        Integer page,
        @NotBlank String text
) {
}
