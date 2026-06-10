package com.exercise.bookmateserver.social;

import com.exercise.bookmateserver.book.BookEntity;
import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.book.BookResponse;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SocialService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    public SocialService(UserRepository userRepository, BookRepository bookRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
    }

    public List<PublicUserResponse> searchUsersByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return List.of();
        }
        return userRepository.findByNicknameContainingIgnoreCaseAndIsPublicTrue(nickname)
                .stream()
                .map(PublicUserResponse::from)
                .collect(Collectors.toList());
    }

    public PublicUserResponse getPublicUserProfile(UUID userId) {
        UserEntity user = userRepository.findByIdAndIsPublicTrue(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or not public"));
        return PublicUserResponse.from(user);
    }

    public List<BookResponse> getPublicUserBooks(UUID userId) {
        // Ensure user exists and is public
        userRepository.findByIdAndIsPublicTrue(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found or not public"));
        
        return bookRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(BookResponse::from)
                .collect(Collectors.toList());
    }

    public PublicUserResponse getRandomPublicUser(UUID currentUserId) {
        UserEntity randomUser = userRepository.findRandomPublicUserExcluding(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No other public users found"));
        return PublicUserResponse.from(randomUser);
    }
}
