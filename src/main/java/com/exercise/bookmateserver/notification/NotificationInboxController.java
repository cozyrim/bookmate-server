package com.exercise.bookmateserver.notification;

import com.exercise.bookmateserver.user.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationInboxController {

    private final NotificationInboxService inboxService;

    public NotificationInboxController(NotificationInboxService inboxService) {
        this.inboxService = inboxService;
    }

    @GetMapping
    public List<NotificationInboxResponse> findNotifications(
            @RequestAttribute("currentUser") UserEntity currentUser
    ) {
        return inboxService.findNotifications(currentUser);
    }

    @GetMapping("/unread-count")
    public NotificationUnreadCountResponse countUnreadNotifications(
            @RequestAttribute("currentUser") UserEntity currentUser
    ) {
        return inboxService.countUnreadNotifications(currentUser);
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(
            @RequestAttribute("currentUser") UserEntity currentUser,
            @PathVariable UUID notificationId
    ) {
        inboxService.markRead(currentUser, notificationId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@RequestAttribute("currentUser") UserEntity currentUser) {
        inboxService.markAllRead(currentUser);
    }
}
