package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserResponse user
) {

    public static AuthResponse of(String accessToken, String refreshToken, UserEntity user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", UserResponse.from(user));
    }
}
