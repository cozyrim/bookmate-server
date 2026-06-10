package com.exercise.bookmateserver.social.guestbook;

import com.exercise.bookmateserver.user.UserEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/social")
public class GuestbookController {

    private final GuestbookService guestbookService;

    public GuestbookController(GuestbookService guestbookService) {
        this.guestbookService = guestbookService;
    }

    @GetMapping("/users/{userId}/guestbook")
    public List<GuestbookMessageResponse> getMessages(@PathVariable UUID userId) {
        return guestbookService.getMessages(userId);
    }

    @PostMapping("/users/{userId}/guestbook")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestbookMessageResponse writeMessage(
            @RequestAttribute("currentUser") UserEntity currentUser,
            @PathVariable UUID userId,
            @Valid @RequestBody GuestbookWriteRequest request
    ) {
        return guestbookService.writeMessage(currentUser, userId, request);
    }

    @DeleteMapping("/guestbook/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(
            @RequestAttribute("currentUser") UserEntity currentUser,
            @PathVariable UUID messageId
    ) {
        guestbookService.deleteMessage(currentUser, messageId);
    }
}
