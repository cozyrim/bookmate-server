package com.exercise.bookmateserver.social.guestbook;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GuestbookRepository extends JpaRepository<GuestbookEntity, UUID> {
    List<GuestbookEntity> findAllByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}
