package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
public class NicknameGenerator {

    private static final List<String> WORDS = List.of(
            "책숲",
            "문장",
            "달빛",
            "페이지",
            "북마크",
            "연필",
            "서재",
            "별책"
    );

    private static final int MAX_ATTEMPTS = 100;

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public NicknameGenerator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String generateUniqueNickname() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String nickname = randomNickname();
            if (!userRepository.existsByNicknameIgnoreCase(nickname)) {
                return nickname;
            }
        }

        throw new IllegalStateException("사용 가능한 랜덤 닉네임을 생성하지 못했습니다.");
    }

    private String randomNickname() {
        String word = WORDS.get(random.nextInt(WORDS.size()));
        int number = random.nextInt(9000) + 1000;
        String nickname = word + number;
        if (NicknamePolicy.length(nickname) > NicknamePolicy.MAX_LENGTH) {
            throw new IllegalStateException("랜덤 닉네임 규칙이 최대 길이를 초과했습니다.");
        }
        return nickname;
    }
}
