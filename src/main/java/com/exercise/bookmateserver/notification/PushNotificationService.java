package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.social.guestbook.GuestbookEntity;
import com.exercise.bookmateserver.user.UserEntity;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private static final String FIREBASE_APP_NAME = "bookmate-fcm";

    private final NotificationDeviceTokenRepository tokenRepository;
    private final NotificationInboxService inboxService;
    private final boolean enabled;
    private final String serviceAccountJson;
    private final String serviceAccountPath;

    private FirebaseMessaging firebaseMessaging;

    public PushNotificationService(
            NotificationDeviceTokenRepository tokenRepository,
            NotificationInboxService inboxService,
            @Value("${app.fcm.enabled:false}") boolean enabled,
            @Value("${app.fcm.service-account-json:}") String serviceAccountJson,
            @Value("${app.fcm.service-account-path:}") String serviceAccountPath
    ) {
        this.tokenRepository = tokenRepository;
        this.inboxService = inboxService;
        this.enabled = enabled;
        this.serviceAccountJson = serviceAccountJson;
        this.serviceAccountPath = serviceAccountPath;
    }

    @PostConstruct
    void initializeFirebaseMessaging() {
        if (!enabled) {
            log.info("FCM push notifications are disabled.");
            return;
        }

        try (InputStream credentialsStream = openCredentialsStream()) {
            if (credentialsStream == null) {
                log.warn("FCM is enabled, but Firebase service account credentials are missing.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsStream))
                    .build();

            FirebaseApp app = findFirebaseApp()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, FIREBASE_APP_NAME));

            this.firebaseMessaging = FirebaseMessaging.getInstance(app);
            log.info("FCM push notification service initialized.");
        } catch (IOException exception) {
            log.error("Failed to initialize FCM push notification service.", exception);
        }
    }

    public void sendGuestbookMessageNotification(
            UserEntity targetUser,
            UserEntity writerUser,
            GuestbookEntity message
    ) {
        if (targetUser.getId().equals(writerUser.getId())) {
            log.info(
                    "Skipping guestbook push notification because writer and target are the same user. userId={} messageId={}",
                    targetUser.getId(),
                    message.getId()
            );
            return;
        }

        String writerNickname = writerUser.getNickname();
        log.info(
                "Preparing guestbook push notification. targetUserId={} writerUserId={} messageId={}",
                targetUser.getId(),
                writerUser.getId(),
                message.getId()
        );

        sendToUser(targetUser.getId(), new PushNotificationPayload(
                "서재에 새 방명록이 도착했어요",
                writerNickname + "님이 방명록을 남겼어요.",
                Map.of(
                        "type", "guestbook_message",
                        "message_id", message.getId().toString(),
                        "target_user_id", targetUser.getId().toString(),
                        "writer_user_id", writerUser.getId().toString()
                )
        ));
    }

    @Transactional
    public void sendToUser(UUID userId, PushNotificationPayload payload) {
        inboxService.recordNotification(userId, payload);

        if (firebaseMessaging == null) {
            log.debug("Skipping push notification because FCM is not initialized. userId={}", userId);
            return;
        }

        List<NotificationDeviceTokenEntity> tokens = tokenRepository.findAllByUser_IdAndEnabledTrue(userId);
        if (tokens.isEmpty()) {
            log.info("No enabled push notification tokens found for user. userId={}", userId);
            return;
        }

        for (NotificationDeviceTokenEntity token : tokens) {
            sendToToken(token, payload);
        }
    }

    private void sendToToken(NotificationDeviceTokenEntity token, PushNotificationPayload payload) {
        Message message = Message.builder()
                .setToken(token.getFcmToken())
                .setNotification(Notification.builder()
                        .setTitle(payload.title())
                        .setBody(payload.body())
                        .build())
                .putAllData(sanitizeData(payload.data()))
                .setApnsConfig(ApnsConfig.builder()
                        .putHeader("apns-priority", "10")
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build())
                .build();

        try {
            String response = firebaseMessaging.send(message);
            log.info("Sent push notification. tokenId={} response={}", token.getId(), response);
        } catch (FirebaseMessagingException exception) {
            handleSendFailure(token, exception);
        }
    }

    private void handleSendFailure(NotificationDeviceTokenEntity token, FirebaseMessagingException exception) {
        MessagingErrorCode errorCode = exception.getMessagingErrorCode();
        log.warn(
                "Failed to send push notification. tokenId={} errorCode={} message={}",
                token.getId(),
                errorCode,
                exception.getMessage()
        );

        if (errorCode == MessagingErrorCode.UNREGISTERED) {
            token.disable();
        }
    }

    private Map<String, String> sanitizeData(Map<String, String> data) {
        Map<String, String> sanitized = new HashMap<>();
        if (data == null) {
            return sanitized;
        }

        data.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private Optional<FirebaseApp> findFirebaseApp() {
        return FirebaseApp.getApps()
                .stream()
                .filter(app -> FIREBASE_APP_NAME.equals(app.getName()))
                .findFirst();
    }

    private InputStream openCredentialsStream() throws IOException {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8));
        }

        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            return new FileInputStream(serviceAccountPath);
        }

        return null;
    }
}
