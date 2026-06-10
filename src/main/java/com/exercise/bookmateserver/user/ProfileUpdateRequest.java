package com.exercise.bookmateserver.user;

import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank String nickname,
        String profileImageUrl,
        Boolean isPublic,
        String roomTheme
) {
}
