package com.exercise.bookmateserver.book;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookUpdateRequest(
        @NotBlank String title,
        @NotBlank String author,
        @NotBlank String imageName,
        String isbn,
        String category,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("1.0")
        Double progress,

        @Min(0)
        Integer totalPages,

        @Min(0)
        Integer currentPage,

        @Min(1) @Max(5)
        Integer rating,

        String review,

        String readingStatus,

        String startDate,

        String endDate
) {
}
