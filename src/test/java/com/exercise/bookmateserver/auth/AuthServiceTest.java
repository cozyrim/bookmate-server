package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.moderation.ContentModerationPolicy;
import com.exercise.bookmateserver.moderation.ModerationService;
import com.exercise.bookmateserver.notification.NotificationDeviceTokenRepository;
import com.exercise.bookmateserver.notification.NotificationInboxRepository;
import com.exercise.bookmateserver.quote.QuoteRepository;
import com.exercise.bookmateserver.readingmemo.ReadingMemoRepository;
import com.exercise.bookmateserver.user.AuthProvider;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import com.exercise.bookmateserver.word.WordRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void refreshRotatesTokensWhenStoredRefreshTokenIsValid() {
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        UserEntity user = mock(UserEntity.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                mock(KakaoClient.class),
                mock(AppleIdentityTokenVerifier.class),
                tokenService,
                mock(NicknameGenerator.class),
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                mock(DiscordLoginNotificationService.class)
        );

        when(tokenService.hashRefreshToken("old-refresh-token")).thenReturn("old-token-hash");
        when(userRepository.findByRefreshTokenHashAndDeletedAtIsNull("old-token-hash"))
                .thenReturn(Optional.of(user));
        when(user.hasValidRefreshToken(org.mockito.ArgumentMatchers.any(Instant.class))).thenReturn(true);
        when(tokenService.createRefreshToken()).thenReturn("new-refresh-token");
        when(tokenService.hashRefreshToken("new-refresh-token")).thenReturn("new-token-hash");
        when(tokenService.refreshTokenExpiresAt()).thenReturn(Instant.parse("2026-10-16T00:00:00Z"));
        when(tokenService.createAccessToken(user)).thenReturn("new-access-token");

        TokenRefreshResponse response = authService.refresh(new RefreshTokenRequest("old-refresh-token"));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
        verify(user).replaceRefreshToken("new-token-hash", Instant.parse("2026-10-16T00:00:00Z"));
    }

    @Test
    void refreshRejectsUnknownRefreshToken() {
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                mock(KakaoClient.class),
                mock(AppleIdentityTokenVerifier.class),
                tokenService,
                mock(NicknameGenerator.class),
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                mock(DiscordLoginNotificationService.class)
        );

        when(tokenService.hashRefreshToken("unknown-refresh-token")).thenReturn("unknown-token-hash");
        when(userRepository.findByRefreshTokenHashAndDeletedAtIsNull("unknown-token-hash"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("unknown-refresh-token")))
                .hasMessageContaining("Refresh token이 유효하지 않습니다.");
    }

    @Test
    void kakaoLoginAllowsMissingEmail() {
        KakaoClient kakaoClient = mock(KakaoClient.class);
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        DiscordLoginNotificationService discordLoginNotificationService = mock(DiscordLoginNotificationService.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                kakaoClient,
                appleIdentityTokenVerifier,
                tokenService,
                mock(NicknameGenerator.class),
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                discordLoginNotificationService
        );

        when(kakaoClient.fetchUserInfo("kakao-access-token"))
                .thenReturn(new KakaoUserInfo("12345", null, "북메이트", null));
        when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByDeletedAtIsNull())
                .thenReturn(1L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.LOCAL))
                .thenReturn(0L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.KAKAO))
                .thenReturn(1L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.APPLE))
                .thenReturn(0L);
        when(tokenService.createAccessToken(any(UserEntity.class)))
                .thenReturn("bookmate-token");

        AuthResponse response = authService.loginWithKakao(new KakaoLoginRequest("kakao-access-token"));

        assertThat(response.accessToken()).isEqualTo("bookmate-token");
        assertThat(response.user().email()).isNull();
        assertThat(response.user().provider().name()).isEqualTo("KAKAO");
        verify(discordLoginNotificationService)
                .notifySignup(eq("kakao"), eq(new SignupStats(1L, 0L, 1L, 0L)));
    }

    @Test
    void appleLoginIgnoresClientEmailAndUsesVerifiedIdentityToken() {
        KakaoClient kakaoClient = mock(KakaoClient.class);
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
        DiscordLoginNotificationService discordLoginNotificationService = mock(DiscordLoginNotificationService.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                kakaoClient,
                appleIdentityTokenVerifier,
                tokenService,
                nicknameGenerator,
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                discordLoginNotificationService
        );

        when(appleIdentityTokenVerifier.verify("apple-identity-token", "apple-user-id"))
                .thenReturn(new AppleUserInfo("apple-user-id", "relay@example.com"));
        when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNicknameIgnoreCase("문장1234"))
                .thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByDeletedAtIsNull())
                .thenReturn(2L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.LOCAL))
                .thenReturn(1L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.KAKAO))
                .thenReturn(0L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.APPLE))
                .thenReturn(1L);
        when(tokenService.createAccessToken(any(UserEntity.class)))
                .thenReturn("bookmate-token");

        AuthResponse response = authService.loginWithApple(new AppleLoginRequest(
                "apple-identity-token",
                "apple-authorization-code",
                "apple-user-id",
                "unverified@example.com",
                "문장1234"
        ));

        assertThat(response.accessToken()).isEqualTo("bookmate-token");
        assertThat(response.user().email()).isEqualTo("relay@example.com");
        assertThat(response.user().provider().name()).isEqualTo("APPLE");
        assertThat(response.user().nickname()).isEqualTo("문장1234");
        verify(discordLoginNotificationService)
                .notifySignup(eq("apple"), eq(new SignupStats(2L, 1L, 0L, 1L)));
    }

    @Test
    void appleLoginSucceedsWhenSignupNotificationFails() {
        KakaoClient kakaoClient = mock(KakaoClient.class);
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        NicknameGenerator nicknameGenerator = mock(NicknameGenerator.class);
        DiscordLoginNotificationService discordLoginNotificationService = mock(DiscordLoginNotificationService.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                kakaoClient,
                appleIdentityTokenVerifier,
                tokenService,
                nicknameGenerator,
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                discordLoginNotificationService
        );

        when(appleIdentityTokenVerifier.verify("apple-identity-token", "apple-user-id"))
                .thenReturn(new AppleUserInfo("apple-user-id", null));
        when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(nicknameGenerator.generateUniqueNickname())
                .thenReturn("북마크1234");
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByDeletedAtIsNull())
                .thenThrow(new RuntimeException("count failed"));
        when(tokenService.createAccessToken(any(UserEntity.class)))
                .thenReturn("bookmate-token");

        AuthResponse response = authService.loginWithApple(new AppleLoginRequest(
                "apple-identity-token",
                "apple-authorization-code",
                "apple-user-id",
                null,
                null
        ));

        assertThat(response.accessToken()).isEqualTo("bookmate-token");
        assertThat(response.user().provider().name()).isEqualTo("APPLE");
        verify(discordLoginNotificationService, never()).notifySignup(any(), any());
        verify(discordLoginNotificationService).notifySignupStatsUnavailable("apple");
    }

    @Test
    void signupSucceedsWhenSignupNotificationFails() {
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        DiscordLoginNotificationService discordLoginNotificationService = mock(DiscordLoginNotificationService.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                mock(KakaoClient.class),
                mock(AppleIdentityTokenVerifier.class),
                tokenService,
                mock(NicknameGenerator.class),
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                discordLoginNotificationService
        );

        when(userRepository.existsByEmailAndDeletedAtIsNull("user@example.com"))
                .thenReturn(false);
        when(userRepository.existsByNicknameIgnoreCase("북메이트"))
                .thenReturn(false);
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByDeletedAtIsNull())
                .thenThrow(new RuntimeException("count failed"));
        when(tokenService.createAccessToken(any(UserEntity.class)))
                .thenReturn("bookmate-token");

        AuthResponse response = authService.signup(new SignupRequest(
                "user@example.com",
                "password123!",
                "북메이트",
                null
        ));

        assertThat(response.accessToken()).isEqualTo("bookmate-token");
        assertThat(response.user().email()).isEqualTo("user@example.com");
        assertThat(response.user().provider().name()).isEqualTo("LOCAL");
        verify(discordLoginNotificationService, never()).notifySignup(any(), any());
        verify(discordLoginNotificationService).notifySignupStatsUnavailable("bookmate");
    }

    @Test
    void signupNotificationRunsAfterCommitWhenTransactionSynchronizationIsActive() {
        KakaoClient kakaoClient = mock(KakaoClient.class);
        AppleIdentityTokenVerifier appleIdentityTokenVerifier = mock(AppleIdentityTokenVerifier.class);
        UserRepository userRepository = mock(UserRepository.class);
        TokenService tokenService = mock(TokenService.class);
        DiscordLoginNotificationService discordLoginNotificationService = mock(DiscordLoginNotificationService.class);
        AuthService authService = new AuthService(
                userRepository,
                mock(BookRepository.class),
                mock(WordRepository.class),
                mock(QuoteRepository.class),
                mock(ReadingMemoRepository.class),
                mock(NotificationDeviceTokenRepository.class),
                mock(NotificationInboxRepository.class),
                kakaoClient,
                appleIdentityTokenVerifier,
                tokenService,
                mock(NicknameGenerator.class),
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class),
                discordLoginNotificationService
        );

        when(kakaoClient.fetchUserInfo("kakao-access-token"))
                .thenReturn(new KakaoUserInfo("12345", null, "북메이트", null));
        when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.countByDeletedAtIsNull())
                .thenReturn(1L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.LOCAL))
                .thenReturn(0L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.KAKAO))
                .thenReturn(1L);
        when(userRepository.countByProviderAndDeletedAtIsNull(AuthProvider.APPLE))
                .thenReturn(0L);
        when(tokenService.createAccessToken(any(UserEntity.class)))
                .thenReturn("bookmate-token");

        try {
            TransactionSynchronizationManager.initSynchronization();

            AuthResponse response = authService.loginWithKakao(new KakaoLoginRequest("kakao-access-token"));

            assertThat(response.accessToken()).isEqualTo("bookmate-token");
            verify(discordLoginNotificationService, never()).notifySignup(any(), any());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(discordLoginNotificationService)
                    .notifySignup(eq("kakao"), eq(new SignupStats(1L, 0L, 1L, 0L)));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
