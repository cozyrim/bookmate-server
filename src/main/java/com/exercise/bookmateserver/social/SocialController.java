package com.exercise.bookmateserver.social;

import com.exercise.bookmateserver.book.BookResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/social/users")
public class SocialController {

    private final SocialService socialService;
    private final CurrentUserResolver currentUserResolver;

    public SocialController(SocialService socialService, CurrentUserResolver currentUserResolver) {
        this.socialService = socialService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/search")
    public ResponseEntity<List<PublicUserResponse>> searchUsers(@RequestParam String nickname) {
        List<PublicUserResponse> users = socialService.searchUsersByNickname(nickname);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PublicUserResponse> getPublicUserProfile(@PathVariable UUID userId) {
        PublicUserResponse user = socialService.getPublicUserProfile(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{userId}/books")
    public ResponseEntity<List<BookResponse>> getPublicUserBooks(@PathVariable UUID userId) {
        List<BookResponse> books = socialService.getPublicUserBooks(userId);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/random")
    public ResponseEntity<PublicUserResponse> getRandomPublicUser(HttpServletRequest request) {
        UserEntity user = currentUserResolver.get(request);
        PublicUserResponse randomUser = socialService.getRandomPublicUser(user.getId());
        return ResponseEntity.ok(randomUser);
    }
}
