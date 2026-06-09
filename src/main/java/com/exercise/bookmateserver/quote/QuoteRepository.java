package com.exercise.bookmateserver.quote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuoteRepository extends JpaRepository<QuoteEntity, UUID> {

    List<QuoteEntity> findByBookIdAndUserIdOrderByCreatedAtDesc(UUID bookId, UUID userId);

    Optional<QuoteEntity> findByIdAndUserId(UUID id, UUID userId);

    void deleteByBookIdAndUserId(UUID bookId, UUID userId);

    void deleteByUserId(UUID userId);
}
