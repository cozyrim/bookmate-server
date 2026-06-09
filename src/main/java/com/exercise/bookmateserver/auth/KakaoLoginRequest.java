package com.exercise.bookmateserver.auth;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank String accessToken
) {
}
