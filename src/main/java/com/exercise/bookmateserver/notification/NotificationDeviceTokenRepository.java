package com.exercise.bookmateserver.notification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceTokenRepository extends JpaRepository<NotificationDeviceTokenEntity, UUID> {

    Optional<NotificationDeviceTokenEntity> findByFcmToken(String fcmToken);

    Optional<NotificationDeviceTokenEntity> findByUser_IdAndFcmToken(UUID userId, String fcmToken);

    List<NotificationDeviceTokenEntity> findAllByUser_IdAndEnabledTrue(UUID userId);

    void deleteByUser_Id(UUID userId);
}
