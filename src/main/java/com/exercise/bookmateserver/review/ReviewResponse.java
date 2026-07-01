package com.exercise.bookmateserver.review;

import com.exercise.bookmateserver.user.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID ownerId,
        String ownerNickname,
        String ownerProfileImageUrl,
        UUID bookId,
        Integer rating,
        String content,
        boolean isPublic,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final String DELETED_USER_DISPLAY_NAME = "알 수 없음";

    public static ReviewResponse from(ReviewEntity review, UserEntity owner) {
        boolean isDeletedOwner = owner.isDeleted();

        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                isDeletedOwner ? DELETED_USER_DISPLAY_NAME : owner.getNickname(),
                isDeletedOwner ? null : owner.getProfileImageUrl(),
                review.getBookId(),
                review.getRating(),
                review.getContent(),
                review.isPublic(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
