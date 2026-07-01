package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.moderation.ContentModerationPolicy;
import com.exercise.bookmateserver.moderation.ModerationService;
import com.exercise.bookmateserver.notification.NotificationDeviceTokenRepository;
import com.exercise.bookmateserver.notification.NotificationInboxRepository;
import com.exercise.bookmateserver.quote.QuoteRepository;
import com.exercise.bookmateserver.readingmemo.ReadingMemoRepository;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import com.exercise.bookmateserver.word.WordRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void kakaoLoginAllowsMissingEmail() {
        KakaoClient kakaoClient = mock(KakaoClient.class);
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
                kakaoClient,
                tokenService,
                mock(NicknameGenerator.class),
                mock(ContentModerationPolicy.class),
                mock(ModerationService.class)
        );

        when(kakaoClient.fetchUserInfo("kakao-access-token"))
                .thenReturn(new KakaoUserInfo("12345", null, "북메이트", null));
        when(userRepository.findByProviderAndProviderIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(UserEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenService.createAccessToken(any(UserEntity.class)))
                .thenReturn("bookmate-token");

        AuthResponse response = authService.loginWithKakao(new KakaoLoginRequest("kakao-access-token"));

        assertThat(response.accessToken()).isEqualTo("bookmate-token");
        assertThat(response.user().email()).isNull();
        assertThat(response.user().provider().name()).isEqualTo("KAKAO");
    }
}
