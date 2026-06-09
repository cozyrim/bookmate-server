package com.exercise.bookmateserver.auth;

public record KakaoUserInfo(
        String providerId,
        String email,
        String nickname,
        String profileImageUrl
) {
}
