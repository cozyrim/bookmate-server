package com.exercise.bookmateserver.social;

import com.exercise.bookmateserver.user.UserEntity;

import java.util.UUID;

public record PublicUserResponse(
        UUID id,
        String nickname,
        String profileImageUrl,
        String roomTheme
) {
    public static PublicUserResponse from(UserEntity user) {
        return new PublicUserResponse(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getRoomTheme()
        );
    }
}
