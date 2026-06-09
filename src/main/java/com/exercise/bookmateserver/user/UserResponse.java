package com.exercise.bookmateserver.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        AuthProvider provider,
        String nickname,
        String profileImageUrl,
        LocalDateTime createdAt,
        boolean isPublic
) {

    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getProvider(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                user.isPublic()
        );
    }
}
