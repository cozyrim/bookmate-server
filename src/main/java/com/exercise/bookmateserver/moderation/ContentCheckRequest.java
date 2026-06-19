package com.exercise.bookmateserver.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContentCheckRequest(
        @NotBlank @Size(max = 2000) String content,
        @NotNull ContentModerationContext context
) {
}
