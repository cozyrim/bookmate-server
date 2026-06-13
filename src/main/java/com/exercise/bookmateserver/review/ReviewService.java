package com.exercise.bookmateserver.review;

import com.exercise.bookmateserver.book.BookEntity;
import com.exercise.bookmateserver.book.BookIsbnNormalizer;
import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookRepository bookRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse saveMyReview(UserEntity user, UUID bookId, ReviewSaveRequest request) {
        validateMyBook(user, bookId);

        ReviewEntity review = reviewRepository.findByBookIdAndUserId(bookId, user.getId())
                .map(existingReview -> {
                    existingReview.update(request.rating(), request.content(), request.isPublic());
                    return existingReview;
                })
                .orElseGet(() -> new ReviewEntity(
                        user.getId(),
                        bookId,
                        request.rating(),
                        request.content(),
                        request.isPublic()
                ));

        ReviewEntity savedReview = reviewRepository.save(review);
        return ReviewResponse.from(savedReview, user);
    }

    public Optional<ReviewResponse> findMyReview(UserEntity user, UUID bookId) {
        validateMyBook(user, bookId);

        return reviewRepository.findByBookIdAndUserId(bookId, user.getId())
                .map(review -> ReviewResponse.from(review, user));
    }

    public List<ReviewResponse> findPublicReviews(UUID bookId) {
        validateBookExists(bookId);

        List<ReviewEntity> reviews = reviewRepository.findAllByBookIdAndIsPublicTrueOrderByUpdatedAtDesc(bookId);
        return toResponses(reviews);
    }

    public List<ReviewResponse> findPublicReviewsByBookIdentity(String isbn, String title, String author) {
        Set<UUID> bookIds = new LinkedHashSet<>();
        String normalizedIsbn = BookIsbnNormalizer.normalize(isbn);

        if (normalizedIsbn != null) {
            bookRepository.findAllByIsbn(normalizedIsbn)
                    .stream()
                    .map(BookEntity::getId)
                    .forEach(bookIds::add);
        }

        if (bookIds.isEmpty() && hasText(title) && hasText(author)) {
            bookRepository.findAllByTitleIgnoreCaseAndAuthorIgnoreCase(title.trim(), author.trim())
                    .stream()
                    .map(BookEntity::getId)
                    .forEach(bookIds::add);
        }

        if (bookIds.isEmpty()) {
            return List.of();
        }

        List<ReviewEntity> reviews = reviewRepository.findAllByBookIdInAndIsPublicTrueOrderByUpdatedAtDesc(bookIds);
        return toResponses(reviews);
    }

    private List<ReviewResponse> toResponses(List<ReviewEntity> reviews) {
        Map<UUID, UserEntity> owners = userRepository.findAllById(
                        reviews.stream()
                                .map(ReviewEntity::getUserId)
                                .collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return reviews.stream()
                .map(review -> ReviewResponse.from(review, findOwner(review, owners)))
                .toList();
    }

    @Transactional
    public void deleteMyReview(UserEntity user, UUID bookId) {
        validateMyBook(user, bookId);
        reviewRepository.deleteByBookIdAndUserId(bookId, user.getId());
    }

    private void validateMyBook(UserEntity user, UUID bookId) {
        bookRepository.findByIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));
    }

    private void validateBookExists(UUID bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private UserEntity findOwner(ReviewEntity review, Map<UUID, UserEntity> owners) {
        UserEntity owner = owners.get(review.getUserId());
        if (owner == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "리뷰 작성자를 찾을 수 없습니다.");
        }

        return owner;
    }
}
