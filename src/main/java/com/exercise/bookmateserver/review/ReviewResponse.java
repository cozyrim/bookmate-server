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

    public static ReviewResponse from(ReviewEntity review, UserEntity owner) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                owner.getNickname(),
                owner.getProfileImageUrl(),
                review.getBookId(),
                review.getRating(),
                review.getContent(),
                review.isPublic(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
