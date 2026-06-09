package com.exercise.bookmateserver.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProfileResponse(
        UUID id,
        String email,
        AuthProvider provider,
        String nickname,
        String profileImageUrl,
        LocalDateTime createdAt,
        long togetherDays,
        long savedWordCount,
        long readBookCount
) {

    public static ProfileResponse of(
            UserEntity user,
            long togetherDays,
            long savedWordCount,
            long readBookCount
    ) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getProvider(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                togetherDays,
                savedWordCount,
                readBookCount
        );
    }
}
