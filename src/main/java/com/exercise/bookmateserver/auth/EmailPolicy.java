package com.exercise.bookmateserver.auth;

import java.util.regex.Pattern;

public final class EmailPolicy {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private EmailPolicy() {
    }

    public static String normalize(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        return email.trim().toLowerCase();
    }

    public static void validate(String email) {
        String normalized = normalize(email);
        if (normalized == null) {
            throw new IllegalArgumentException("이메일을 입력해주세요.");
        }

        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("올바른 이메일 형식으로 입력해주세요.");
        }
    }
}
