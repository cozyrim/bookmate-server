package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.user.AuthProvider;
import com.exercise.bookmateserver.user.ProfileResponse;
import com.exercise.bookmateserver.user.ProfileUpdateRequest;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import com.exercise.bookmateserver.word.WordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final WordRepository wordRepository;
    private final KakaoClient kakaoClient;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            BookRepository bookRepository,
            WordRepository wordRepository,
            KakaoClient kakaoClient,
            TokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.wordRepository = wordRepository;
        this.kakaoClient = kakaoClient;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 가입된 이메일입니다.");
        }

        UserEntity user = UserEntity.createLocal(
                email,
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.profileImageUrl()
        );

        UserEntity savedUser = userRepository.save(user);
        return AuthResponse.of(tokenService.createAccessToken(savedUser), savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return AuthResponse.of(tokenService.createAccessToken(user), user);
    }

    @Transactional
    public AuthResponse loginWithKakao(KakaoLoginRequest request) {
        KakaoUserInfo kakaoUserInfo = kakaoClient.fetchUserInfo(request.accessToken());

        UserEntity user = userRepository
                .findByProviderAndProviderId(AuthProvider.KAKAO, kakaoUserInfo.providerId())
                .map(existingUser -> {
                    existingUser.updateKakaoAccountInfo(kakaoUserInfo.email());
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(UserEntity.createKakao(
                        kakaoUserInfo.providerId(),
                        kakaoUserInfo.email(),
                        kakaoUserInfo.nickname(),
                        kakaoUserInfo.profileImageUrl()
                )));

        return AuthResponse.of(tokenService.createAccessToken(user), user);
    }

    public ProfileResponse getProfile(UserEntity user) {
        return createProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(UserEntity user, ProfileUpdateRequest request) {
        user.updateProfile(request.nickname(), request.profileImageUrl(), request.isPublic());
        UserEntity savedUser = userRepository.saveAndFlush(user);
        return createProfileResponse(savedUser);
    }

    @Transactional
    public void deleteAccount(UserEntity user) {
        wordRepository.deleteByUserId(user.getId());
        bookRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
    }

    private ProfileResponse createProfileResponse(UserEntity user) {
        long togetherDays = ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now()) + 1;
        long savedWordCount = wordRepository.countByUserId(user.getId());
        long readBookCount = bookRepository.countByUserIdAndProgressGreaterThanEqual(user.getId(), 1.0);

        return ProfileResponse.of(user, togetherDays, savedWordCount, readBookCount);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
