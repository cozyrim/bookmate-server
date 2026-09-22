package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService(
            new ObjectMapper(),
            "token-service-test-secret-at-least-32-bytes",
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
    @Test
    void rejectsMissingWeakAndPlaceholderSigningKeys() {
        for (String secret : new String[] {"", "short", "bookmate-local-development-secret-change-me",
                "change-this-to-a-long-random-secret-at-least-32-characters"}) {
            assertThatThrownBy(() -> new TokenService(new ObjectMapper(), secret, 60, 120))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void rejectsTamperedAndExpiredTokens() {
        UserEntity user = mock(UserEntity.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        String valid = tokenService.createAccessToken(user);
        assertThatThrownBy(() -> tokenService.parseUserId(valid + "tampered"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        TokenService expired = new TokenService(new ObjectMapper(),
                "token-service-test-secret-at-least-32-bytes", -10, 120);
        assertThatThrownBy(() -> tokenService.parseUserId(expired.createAccessToken(user)))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }

}
