package com.exercise.bookmateserver.moderation;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserBlockResponse(
        UUID id,
        UUID blockerUserId,
        UUID blockedUserId,
        LocalDateTime createdAt
) {

    public static UserBlockResponse from(UserBlockEntity block) {
        return new UserBlockResponse(
                block.getId(),
                block.getBlockerUserId(),
                block.getBlockedUserId(),
                block.getCreatedAt()
        );
    }
}
