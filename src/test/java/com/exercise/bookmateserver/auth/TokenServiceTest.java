package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService(
            new ObjectMapper(),
            "token-service-test-secret",
            60,
            120
    );

    @Test
    void createsVerifiableAccessTokenAndOpaqueRefreshToken() {
        UUID userId = UUID.randomUUID();
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(userId);

        String accessToken = tokenService.createAccessToken(user);
        String refreshToken = tokenService.createRefreshToken();

        assertThat(tokenService.parseUserId(accessToken)).isEqualTo(userId);
        assertThat(refreshToken).doesNotContain(".");
        assertThat(tokenService.hashRefreshToken(refreshToken))
                .isEqualTo(tokenService.hashRefreshToken(refreshToken));
        assertThat(tokenService.refreshTokenExpiresAt()).isAfter(java.time.Instant.now());
    }
}
