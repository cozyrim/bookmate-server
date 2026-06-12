package com.exercise.bookmateserver.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewSaveRequest(
        @NotNull
        @Min(0)
        @Max(5)
        Integer rating,

        @NotNull
        String content,

        boolean isPublic
) {
}
