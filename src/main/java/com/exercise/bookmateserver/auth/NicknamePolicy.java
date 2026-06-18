package com.exercise.bookmateserver.auth;

import java.util.regex.Pattern;

public final class NicknamePolicy {

    public static final int MAX_LENGTH = 8;
    public static final String RULE_MESSAGE = "닉네임은 8자 이하, 한글/영문/숫자만 사용할 수 있어요.";
    private static final Pattern NICKNAME_PATTERN = Pattern.compile("^[가-힣A-Za-z0-9]+$");

    private NicknamePolicy() {
    }

    public static String normalize(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return null;
        }

        return nickname.trim();
    }

    public static void validate(String nickname) {
        String normalized = normalize(nickname);
        if (normalized == null) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        if (length(normalized) > MAX_LENGTH) {
            throw new IllegalArgumentException("닉네임은 8자 이하로 입력해주세요.");
        }

        if (!NICKNAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(RULE_MESSAGE);
        }
    }

    public static int length(String value) {
        return value.codePointCount(0, value.length());
    }
}
