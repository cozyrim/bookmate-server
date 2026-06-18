package com.exercise.bookmateserver.auth;

import java.util.regex.Pattern;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final String RULE_MESSAGE = "비밀번호는 8자 이상, 소문자, 숫자, 특수문자를 각각 1개 이상 포함해야 해요.";

    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
    private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        if (password.length() < MIN_LENGTH
                || WHITESPACE_PATTERN.matcher(password).find()
                || !LOWERCASE_PATTERN.matcher(password).find()
                || !DIGIT_PATTERN.matcher(password).find()
                || !SPECIAL_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException(RULE_MESSAGE);
        }
    }
}
