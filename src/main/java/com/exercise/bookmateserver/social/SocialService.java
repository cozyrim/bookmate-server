package com.exercise.bookmateserver.social;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.book.BookResponse;
import com.exercise.bookmateserver.moderation.ModerationService;
import com.exercise.bookmateserver.moderation.ModerationTargetType;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ModerationService moderationService;

    public SocialService(
            UserRepository userRepository,
            BookRepository bookRepository,
            ModerationService moderationService
    ) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.moderationService = moderationService;
    }

    public List<PublicUserResponse> searchUsersByNickname(UserEntity viewer, String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return List.of();
        }
        return userRepository.findByNicknameContainingIgnoreCaseAndIsPublicTrueAndDeletedAtIsNull(nickname)
                .stream()
                .filter(candidate -> isVisiblePublicUser(viewer, candidate))
                .map(PublicUserResponse::from)
                .collect(Collectors.toList());
    }

    public PublicUserResponse getPublicUserProfile(UserEntity viewer, UUID userId) {
        UserEntity user = userRepository.findByIdAndIsPublicTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or not public"));
        validateVisiblePublicUser(viewer, user, ModerationTargetType.PUBLIC_PROFILE);
        return PublicUserResponse.from(user);
    }

    public List<BookResponse> getPublicUserBooks(UserEntity viewer, UUID userId) {
        UserEntity publicUser = userRepository.findByIdAndIsPublicTrueAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or not public"));
        validateVisiblePublicUser(viewer, publicUser, ModerationTargetType.PUBLIC_BOOKSHELF);

        return bookRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(book -> !moderationService.shouldHideTargetForUser(
                        viewer.getId(),
                        ModerationTargetType.PUBLIC_BOOK,
                        book.getId().toString()
                ))
                .map(BookResponse::from)
                .collect(Collectors.toList());
    }

    public PublicUserResponse getRandomPublicUser(UserEntity viewer) {
        List<UserEntity> candidates = userRepository.findByIsPublicTrueAndDeletedAtIsNull()
                .stream()
                .filter(candidate -> !candidate.getId().equals(viewer.getId()))
                .filter(candidate -> isVisiblePublicUser(viewer, candidate))
                .collect(Collectors.toList());

        Collections.shuffle(candidates);

        UserEntity randomUser = candidates.stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No other public users found"));
        return PublicUserResponse.from(randomUser);
    }

    private boolean isVisiblePublicUser(UserEntity viewer, UserEntity candidate) {
        if (candidate.getId().equals(viewer.getId())) {
            return true;
        }

        return !moderationService.hasBlockBetween(viewer.getId(), candidate.getId())
                && !moderationService.shouldHideTargetForUser(
                viewer.getId(),
                ModerationTargetType.PUBLIC_PROFILE,
                candidate.getId().toString()
        )
                && !moderationService.shouldHideTargetForUser(
                viewer.getId(),
                ModerationTargetType.PUBLIC_BOOKSHELF,
                candidate.getId().toString()
        );
    }

    private void validateVisiblePublicUser(
            UserEntity viewer,
            UserEntity publicUser,
            ModerationTargetType targetType
    ) {
        if (viewer.getId().equals(publicUser.getId())) {
            return;
        }

        moderationService.ensureNoBlockBetween(viewer.getId(), publicUser.getId());
        if (moderationService.shouldHideTargetForUser(viewer.getId(), targetType, publicUser.getId().toString())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or not public");
        }
    }
}
