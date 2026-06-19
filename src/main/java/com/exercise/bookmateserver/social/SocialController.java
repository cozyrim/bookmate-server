package com.exercise.bookmateserver.social;

import com.exercise.bookmateserver.book.BookResponse;
import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
    public ResponseEntity<List<PublicUserResponse>> searchUsers(HttpServletRequest request, @RequestParam String nickname) {
        UserEntity user = currentUserResolver.get(request);
        List<PublicUserResponse> users = socialService.searchUsersByNickname(user, nickname);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PublicUserResponse> getPublicUserProfile(HttpServletRequest request, @PathVariable UUID userId) {
        UserEntity currentUser = currentUserResolver.get(request);
        PublicUserResponse publicUser = socialService.getPublicUserProfile(currentUser, userId);
        return ResponseEntity.ok(publicUser);
    }

    @GetMapping("/{userId}/books")
    public ResponseEntity<List<BookResponse>> getPublicUserBooks(HttpServletRequest request, @PathVariable UUID userId) {
        UserEntity currentUser = currentUserResolver.get(request);
        List<BookResponse> books = socialService.getPublicUserBooks(currentUser, userId);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/random")
    public ResponseEntity<PublicUserResponse> getRandomPublicUser(HttpServletRequest request) {
        UserEntity user = currentUserResolver.get(request);
        PublicUserResponse randomUser = socialService.getRandomPublicUser(user);
        return ResponseEntity.ok(randomUser);
    }
}
