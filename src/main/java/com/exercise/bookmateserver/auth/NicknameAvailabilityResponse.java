package com.exercise.bookmateserver.auth;

public record NicknameAvailabilityResponse(
        String nickname,
        boolean available,
        String message
) {
}
