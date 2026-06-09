package com.exercise.bookmateserver.quote;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record QuoteCreateRequest(
        @NotNull UUID bookId,
        @NotBlank String text,
        @Min(1) Integer page,
        String memo
) {
}
