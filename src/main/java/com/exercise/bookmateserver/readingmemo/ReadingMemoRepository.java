package com.exercise.bookmateserver.readingmemo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReadingMemoRepository extends JpaRepository<ReadingMemoEntity, UUID> {
    List<ReadingMemoEntity> findAllByBookIdAndUserIdOrderByDateAscCreatedAtAsc(UUID bookId, UUID userId);
    Optional<ReadingMemoEntity> findByIdAndUserId(UUID id, UUID userId);
    void deleteByBookIdAndUserId(UUID bookId, UUID userId);
}
