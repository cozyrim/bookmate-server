package com.exercise.bookmateserver.auth;

import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
        @NotBlank String identityToken,
        String authorizationCode,
        @NotBlank String userIdentifier,
        String email,
        String fullName
) {
}
