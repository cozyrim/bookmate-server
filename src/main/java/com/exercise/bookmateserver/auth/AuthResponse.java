package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {

    public static AuthResponse of(String accessToken, UserEntity user) {
        return new AuthResponse(accessToken, "Bearer", UserResponse.from(user));
    }
}
