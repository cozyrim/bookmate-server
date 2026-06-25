package com.exercise.bookmateserver.book;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<BookEntity, UUID> {

    List<BookEntity> findAllByOrderByCreatedAtDesc();

    List<BookEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<BookEntity> findAllByIsbn(String isbn);

    List<BookEntity> findAllByTitleIgnoreCaseAndAuthorIgnoreCase(String title, String author);

    List<BookEntity> findAllByUserIdAndIsbnOrderByCreatedAtDesc(UUID userId, String isbn);

    List<BookEntity> findAllByUserIdAndTitleIgnoreCaseAndAuthorIgnoreCaseOrderByCreatedAtDesc(
            UUID userId,
            String title,
            String author
    );

    Optional<BookEntity> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);

    long countByUserIdAndProgressGreaterThanEqual(UUID userId, Double progress);

    void deleteByUserId(UUID userId);
}
