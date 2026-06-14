package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

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

    private static final int MAX_ATTEMPTS = 20;

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

        return "북메이트" + UUID.randomUUID().toString().substring(0, 6);
    }

    private String randomNickname() {
        String word = WORDS.get(random.nextInt(WORDS.size()));
        int number = random.nextInt(9000) + 1000;
        return word + number;
    }
}
