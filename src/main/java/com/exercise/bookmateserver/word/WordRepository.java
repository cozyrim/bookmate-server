package com.exercise.bookmateserver.word;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

// 디비에 저장하고 조회하는 역할
// findByBookIdOrderByCreatedAtDesc는 Spring Data JPA가 메서드 이름을 보고 자동으로 쿼리를 만듦

public interface WordRepository extends JpaRepository<WordEntity, UUID> {

    List<WordEntity> findAllByOrderByCreatedAtDesc();

    List<WordEntity> findByBookIdOrderByCreatedAtDesc(UUID bookId);

    void deleteByBookId(UUID bookId);

    List<WordEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<WordEntity> findByBookIdAndUserIdOrderByCreatedAtDesc(UUID bookId, UUID userId);

    java.util.Optional<WordEntity> findByIdAndUserId(UUID id, UUID userId);

    void deleteByBookIdAndUserId(UUID bookId, UUID userId);

    long countByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
