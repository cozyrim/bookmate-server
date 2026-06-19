package com.exercise.bookmateserver.moderation;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ContentModerationPolicy {

    private static final int MAX_PUBLIC_TEXT_LENGTH = 1000;
    private static final Pattern PHONE_PATTERN = Pattern.compile("(01[016789])[- .]?\\d{3,4}[- .]?\\d{4}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://|www\\.)\\S+", Pattern.CASE_INSENSITIVE);
    private static final List<String> BLOCKED_TERMS = List.of(
            "씨발",
            "시발",
            "ㅅㅂ",
            "병신",
            "개새끼",
            "죽어",
            "꺼져"
    );

    public ContentCheckResponse check(String content, ContentModerationContext context) {
        List<String> reasons = new ArrayList<>();

        if (content == null || content.isBlank()) {
            return new ContentCheckResponse(true, List.of());
        }

        String normalized = content.trim();
        if (normalized.length() > maxLength(context)) {
            reasons.add("내용이 너무 길어요.");
        }

        String compact = normalized
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");

        for (String blockedTerm : BLOCKED_TERMS) {
            if (compact.contains(blockedTerm)) {
                reasons.add("부적절한 표현이 포함되어 있어요.");
                break;
            }
        }

        if (PHONE_PATTERN.matcher(normalized).find() || EMAIL_PATTERN.matcher(normalized).find()) {
            reasons.add("개인 연락처가 포함된 것 같아요.");
        }

        if (URL_PATTERN.matcher(normalized).find() && context != ContentModerationContext.BOOK_REVIEW) {
            reasons.add("공개 공간에는 외부 링크를 남길 수 없어요.");
        }

        return new ContentCheckResponse(reasons.isEmpty(), reasons);
    }

    public void validateAllowed(String content, ContentModerationContext context) {
        ContentCheckResponse result = check(content, context);
        if (!result.allowed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.reasons().get(0));
        }
    }

    private int maxLength(ContentModerationContext context) {
        if (context == ContentModerationContext.PROFILE) {
            return 20;
        }

        return MAX_PUBLIC_TEXT_LENGTH;
    }
}
