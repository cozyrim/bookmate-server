package com.exercise.bookmateserver.social.guestbook;

import java.time.LocalDateTime;
import java.util.UUID;

public record GuestbookMessageResponse(
        UUID id,
        UUID writerId,
        String writerNickname,
        String writerProfileImageUrl,
        String content,
        LocalDateTime createdAt
) {
    private static final String DELETED_USER_DISPLAY_NAME = "알 수 없음";

    public static GuestbookMessageResponse from(GuestbookEntity entity) {
        boolean isDeletedWriter = entity.getWriterUser().isDeleted();

        return new GuestbookMessageResponse(
                entity.getId(),
                entity.getWriterUser().getId(),
                isDeletedWriter ? DELETED_USER_DISPLAY_NAME : entity.getWriterUser().getNickname(),
                isDeletedWriter ? null : entity.getWriterUser().getProfileImageUrl(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
