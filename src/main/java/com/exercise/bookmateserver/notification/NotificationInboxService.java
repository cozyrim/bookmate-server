package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationInboxService {

    private static final Logger log = LoggerFactory.getLogger(NotificationInboxService.class);

    private final NotificationInboxRepository inboxRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public NotificationInboxService(
            NotificationInboxRepository inboxRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.inboxRepository = inboxRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public List<NotificationInboxResponse> findNotifications(UserEntity user) {
        return inboxRepository.findTop50ByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(entity -> NotificationInboxResponse.from(entity, deserializeData(entity.getDataJson())))
                .toList();
    }

    public NotificationUnreadCountResponse countUnreadNotifications(UserEntity user) {
        long unreadCount = inboxRepository.countByUser_IdAndReadFalse(user.getId());
        return new NotificationUnreadCountResponse(Math.toIntExact(unreadCount));
    }

    @Transactional
    public void recordNotification(UUID userId, PushNotificationPayload payload) {
        UserEntity user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            log.warn("Skipping notification inbox record because user was not found. userId={}", userId);
            return;
        }

        inboxRepository.save(new NotificationInboxEntity(user, payload, serializeData(payload.data())));
    }

    @Transactional
    public void markRead(UserEntity user, UUID notificationId) {
        NotificationInboxEntity notification = inboxRepository.findByIdAndUser_Id(notificationId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));

        notification.markRead();
    }

    @Transactional
    public void markAllRead(UserEntity user) {
        inboxRepository.findAllByUser_IdAndReadFalse(user.getId())
                .forEach(NotificationInboxEntity::markRead);
    }

    private String serializeData(Map<String, String> data) {
        try {
            return objectMapper.writeValueAsString(data == null ? Collections.emptyMap() : data);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize notification data.", exception);
            return "{}";
        }
    }

    private Map<String, String> deserializeData(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(dataJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize notification data. notificationData={}", dataJson, exception);
            return Collections.emptyMap();
        }
    }
}
