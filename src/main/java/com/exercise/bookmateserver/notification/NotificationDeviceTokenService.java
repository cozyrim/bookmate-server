package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.user.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationDeviceTokenService {

    private final NotificationDeviceTokenRepository tokenRepository;

    public NotificationDeviceTokenService(NotificationDeviceTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public NotificationDeviceTokenResponse upsertDeviceToken(
            UserEntity user,
            NotificationDeviceTokenUpsertRequest request
    ) {
        NotificationDeviceTokenEntity token = tokenRepository.findByFcmToken(request.token().trim())
                .map(existingToken -> {
                    existingToken.refresh(user, request.platform(), request.deviceId(), request.appVersion());
                    return existingToken;
                })
                .orElseGet(() -> new NotificationDeviceTokenEntity(
                        user,
                        request.token(),
                        request.platform(),
                        request.deviceId(),
                        request.appVersion()
                ));

        return NotificationDeviceTokenResponse.from(tokenRepository.save(token));
    }

    @Transactional
    public void disableDeviceToken(UserEntity user, String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        tokenRepository.findByUser_IdAndFcmToken(user.getId(), fcmToken.trim())
                .ifPresent(NotificationDeviceTokenEntity::disable);
    }
}
