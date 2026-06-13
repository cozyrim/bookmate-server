package com.exercise.bookmateserver.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    Optional<ReviewEntity> findByBookIdAndUserId(UUID bookId, UUID userId);

    Optional<ReviewEntity> findByIdAndUserId(UUID id, UUID userId);

    List<ReviewEntity> findAllByBookIdAndIsPublicTrueOrderByUpdatedAtDesc(UUID bookId);

    List<ReviewEntity> findAllByBookIdInAndIsPublicTrueOrderByUpdatedAtDesc(Collection<UUID> bookIds);

    void deleteByBookIdAndUserId(UUID bookId, UUID userId);

    void deleteByUserId(UUID userId);
}
