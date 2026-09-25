package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class LogoutIntegrationTest {
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired TokenService tokens;

    @Test
    void revokedCredentialCannotRefreshAndRepeatedLogoutIsSafe() {
        String credential = tokens.createRefreshToken();
        UserEntity user = UserEntity.createLocal(UUID.randomUUID()+"@example.invalid", "unused", "로그아웃", null);
        user.replaceRefreshToken(tokens.hashRefreshToken(credential), Instant.now().plusSeconds(3600));
        users.saveAndFlush(user);
        auth.logout(new RefreshTokenRequest(credential));
        users.flush();
        assertThat(user.hasValidRefreshToken(Instant.now())).isFalse();
        assertThatThrownBy(() -> auth.refresh(new RefreshTokenRequest(credential)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatCode(() -> auth.logout(new RefreshTokenRequest(credential))).doesNotThrowAnyException();
    }

    @Test
    void staleLogoutDoesNotRevokeRotatedCredential() {
        String old = tokens.createRefreshToken();
        UserEntity user = UserEntity.createLocal(UUID.randomUUID()+"@example.invalid", "unused", "새세션", null);
        user.replaceRefreshToken(tokens.hashRefreshToken(old), Instant.now().plusSeconds(3600));
        users.saveAndFlush(user);
        TokenRefreshResponse rotated = auth.refresh(new RefreshTokenRequest(old));
        users.flush();
        auth.logout(new RefreshTokenRequest(old));
        assertThatCode(() -> auth.refresh(new RefreshTokenRequest(rotated.refreshToken())))
                .doesNotThrowAnyException();
    }
}
