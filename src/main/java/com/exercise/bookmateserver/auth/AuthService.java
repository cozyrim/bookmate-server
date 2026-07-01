package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.moderation.ContentModerationContext;
import com.exercise.bookmateserver.moderation.ContentModerationPolicy;
import com.exercise.bookmateserver.moderation.ModerationService;
import com.exercise.bookmateserver.notification.NotificationDeviceTokenRepository;
import com.exercise.bookmateserver.notification.NotificationInboxRepository;
import com.exercise.bookmateserver.quote.QuoteRepository;
import com.exercise.bookmateserver.readingmemo.ReadingMemoRepository;
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
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String EMAIL_DUPLICATED_MESSAGE = "이미 가입된 이메일입니다.";
    private static final String EMAIL_AVAILABLE_MESSAGE = "가입 가능한 이메일입니다.";
    private static final String NICKNAME_REQUIRED_MESSAGE = "닉네임을 입력해주세요.";
    private static final String NICKNAME_TOO_LONG_MESSAGE = "닉네임은 8자 이하로 입력해주세요.";
    private static final String NICKNAME_DUPLICATED_MESSAGE = "이미 사용 중인 닉네임입니다.";
    private static final String NICKNAME_AVAILABLE_MESSAGE = "사용 가능한 닉네임입니다.";

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final WordRepository wordRepository;
    private final QuoteRepository quoteRepository;
    private final ReadingMemoRepository readingMemoRepository;
    private final NotificationDeviceTokenRepository notificationDeviceTokenRepository;
    private final NotificationInboxRepository notificationInboxRepository;
    private final KakaoClient kakaoClient;
    private final TokenService tokenService;
    private final NicknameGenerator nicknameGenerator;
    private final ContentModerationPolicy contentModerationPolicy;
    private final ModerationService moderationService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            BookRepository bookRepository,
            WordRepository wordRepository,
            QuoteRepository quoteRepository,
            ReadingMemoRepository readingMemoRepository,
            NotificationDeviceTokenRepository notificationDeviceTokenRepository,
            NotificationInboxRepository notificationInboxRepository,
            KakaoClient kakaoClient,
            TokenService tokenService,
            NicknameGenerator nicknameGenerator,
            ContentModerationPolicy contentModerationPolicy,
            ModerationService moderationService
    ) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.wordRepository = wordRepository;
        this.quoteRepository = quoteRepository;
        this.readingMemoRepository = readingMemoRepository;
        this.notificationDeviceTokenRepository = notificationDeviceTokenRepository;
        this.notificationInboxRepository = notificationInboxRepository;
        this.kakaoClient = kakaoClient;
        this.tokenService = tokenService;
        this.nicknameGenerator = nicknameGenerator;
        this.contentModerationPolicy = contentModerationPolicy;
        this.moderationService = moderationService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        String email = EmailPolicy.normalize(request.email());
        String nickname = NicknamePolicy.normalize(request.nickname());

        validateEmailAvailable(email);
        validatePassword(request.password());

        if (nickname == null) {
            nickname = nicknameGenerator.generateUniqueNickname();
        } else {
            validateNicknameAvailable(nickname);
        }

        UserEntity user = UserEntity.createLocal(
                email,
                passwordEncoder.encode(request.password()),
                nickname,
                request.profileImageUrl()
        );

        UserEntity savedUser = userRepository.save(user);
        return AuthResponse.of(tokenService.createAccessToken(savedUser), savedUser);
    }

    public NicknameSuggestionResponse suggestNickname() {
        return new NicknameSuggestionResponse(nicknameGenerator.generateUniqueNickname());
    }

    public EmailAvailabilityResponse checkEmailAvailability(String requestedEmail) {
        String email = EmailPolicy.normalize(requestedEmail);
        if (email == null) {
            return new EmailAvailabilityResponse("", false, "이메일을 입력해주세요.");
        }

        try {
            EmailPolicy.validate(email);
        } catch (IllegalArgumentException exception) {
            return new EmailAvailabilityResponse(email, false, exception.getMessage());
        }

        boolean available = !userRepository.existsByEmailAndDeletedAtIsNull(email);
        return new EmailAvailabilityResponse(
                email,
                available,
                available ? EMAIL_AVAILABLE_MESSAGE : EMAIL_DUPLICATED_MESSAGE
        );
    }

    public NicknameAvailabilityResponse checkNicknameAvailability(String requestedNickname) {
        String nickname = NicknamePolicy.normalize(requestedNickname);
        if (nickname == null) {
            return new NicknameAvailabilityResponse("", false, NICKNAME_REQUIRED_MESSAGE);
        }

        try {
            NicknamePolicy.validate(nickname);
        } catch (IllegalArgumentException exception) {
            return new NicknameAvailabilityResponse(nickname, false, exception.getMessage());
        }

        boolean available = !userRepository.existsByNicknameIgnoreCase(nickname);
        return new NicknameAvailabilityResponse(
                nickname,
                available,
                available ? NICKNAME_AVAILABLE_MESSAGE : NICKNAME_DUPLICATED_MESSAGE
        );
    }

    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailAndDeletedAtIsNull(EmailPolicy.normalize(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return AuthResponse.of(tokenService.createAccessToken(user), user);
    }

    @Transactional
    public AuthResponse loginWithKakao(KakaoLoginRequest request) {
        KakaoUserInfo kakaoUserInfo = kakaoClient.fetchUserInfo(request.accessToken());
        String email = EmailPolicy.normalize(kakaoUserInfo.email());

        UserEntity user = userRepository
                .findByProviderAndProviderIdAndDeletedAtIsNull(AuthProvider.KAKAO, kakaoUserInfo.providerId())
                .map(existingUser -> {
                    existingUser.updateKakaoProfile(
                            email,
                            resolveKakaoNickname(kakaoUserInfo.nickname()),
                            kakaoUserInfo.profileImageUrl()
                    );
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(UserEntity.createKakao(
                        kakaoUserInfo.providerId(),
                        email,
                        resolveKakaoNickname(kakaoUserInfo.nickname()),
                        kakaoUserInfo.profileImageUrl()
                )));

        return AuthResponse.of(tokenService.createAccessToken(user), user);
    }

    public ProfileResponse getProfile(UserEntity user) {
        return createProfileResponse(user);
    }

    @Transactional
    public ProfileResponse updateProfile(UserEntity user, ProfileUpdateRequest request) {
        String nickname = NicknamePolicy.normalize(request.nickname());
        validateNicknameAvailableForUpdate(nickname, user);
        contentModerationPolicy.validateAllowed(nickname, ContentModerationContext.PROFILE);

        user.updateProfile(nickname, request.profileImageUrl(), request.isPublic(), request.roomTheme());
        UserEntity savedUser = userRepository.saveAndFlush(user);
        return createProfileResponse(savedUser);
    }

    @Transactional
    public void deleteAccount(UserEntity user) {
        UUID userId = user.getId();

        notificationDeviceTokenRepository.deleteByUser_Id(userId);
        notificationInboxRepository.deleteByUser_Id(userId);
        moderationService.deleteUserModerationData(userId);
        readingMemoRepository.deleteByUserId(userId);
        quoteRepository.deleteByUserId(userId);
        wordRepository.deleteByUserId(userId);
        user.anonymizeForAccountDeletion();
        userRepository.saveAndFlush(user);
    }

    private ProfileResponse createProfileResponse(UserEntity user) {
        long togetherDays = ChronoUnit.DAYS.between(user.getCreatedAt().toLocalDate(), LocalDate.now()) + 1;
        long savedWordCount = wordRepository.countByUserId(user.getId());
        long readBookCount = bookRepository.countByUserIdAndProgressGreaterThanEqual(user.getId(), 1.0);

        return ProfileResponse.of(user, togetherDays, savedWordCount, readBookCount);
    }

    private void validateEmailAvailable(String email) {
        try {
            EmailPolicy.validate(email);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, EMAIL_DUPLICATED_MESSAGE);
        }
    }

    private void validatePassword(String password) {
        try {
            PasswordPolicy.validate(password);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    private void validateNicknameAvailable(String nickname) {
        try {
            NicknamePolicy.validate(nickname);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        if (userRepository.existsByNicknameIgnoreCase(nickname)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, NICKNAME_DUPLICATED_MESSAGE);
        }
    }

    private void validateNicknameAvailableForUpdate(String nickname, UserEntity user) {
        try {
            NicknamePolicy.validate(nickname);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }

        if (!nickname.equalsIgnoreCase(user.getNickname())
                && userRepository.existsByNicknameIgnoreCaseAndIdNot(nickname, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, NICKNAME_DUPLICATED_MESSAGE);
        }
    }

    private String resolveKakaoNickname(String requestedNickname) {
        String nickname = NicknamePolicy.normalize(requestedNickname);
        if (nickname == null || userRepository.existsByNicknameIgnoreCase(nickname)) {
            return nicknameGenerator.generateUniqueNickname();
        }

        try {
            NicknamePolicy.validate(nickname);
        } catch (IllegalArgumentException exception) {
            return nicknameGenerator.generateUniqueNickname();
        }

        return nickname;
    }
}
