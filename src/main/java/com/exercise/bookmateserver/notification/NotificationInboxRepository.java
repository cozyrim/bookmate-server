package com.exercise.bookmateserver.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationInboxRepository extends JpaRepository<NotificationInboxEntity, UUID> {

    List<NotificationInboxEntity> findTop50ByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<NotificationInboxEntity> findAllByUser_IdAndReadFalse(UUID userId);

    long countByUser_IdAndReadFalse(UUID userId);

    Optional<NotificationInboxEntity> findByIdAndUser_Id(UUID id, UUID userId);

    void deleteByUser_Id(UUID userId);
}
