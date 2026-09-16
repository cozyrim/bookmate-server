package com.exercise.bookmateserver.auth;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {

    public static TokenRefreshResponse of(String accessToken, String refreshToken) {
        return new TokenRefreshResponse(accessToken, refreshToken, "Bearer");
    }
}
