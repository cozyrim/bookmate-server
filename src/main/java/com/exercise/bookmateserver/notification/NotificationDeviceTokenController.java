package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.user.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationDeviceTokenController {

    private final NotificationDeviceTokenService tokenService;

    public NotificationDeviceTokenController(NotificationDeviceTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/device-token")
    public NotificationDeviceTokenResponse upsertDeviceToken(
            @RequestAttribute("currentUser") UserEntity currentUser,
            @Valid @RequestBody NotificationDeviceTokenUpsertRequest request
    ) {
        return tokenService.upsertDeviceToken(currentUser, request);
    }

    @DeleteMapping("/device-token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disableDeviceToken(
            @RequestAttribute("currentUser") UserEntity currentUser,
            @RequestParam String token
    ) {
        tokenService.disableDeviceToken(currentUser, token);
    }
}
