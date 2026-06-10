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
    public static GuestbookMessageResponse from(GuestbookEntity entity) {
        return new GuestbookMessageResponse(
                entity.getId(),
                entity.getWriterUser().getId(),
                entity.getWriterUser().getNickname(),
                entity.getWriterUser().getProfileImageUrl(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
