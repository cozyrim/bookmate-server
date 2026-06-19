package com.exercise.bookmateserver.review;

import com.exercise.bookmateserver.book.BookEntity;
import com.exercise.bookmateserver.book.BookIsbnNormalizer;
import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.moderation.ContentModerationContext;
import com.exercise.bookmateserver.moderation.ModerationService;
import com.exercise.bookmateserver.moderation.ModerationTargetType;
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
    private final ModerationService moderationService;

    public ReviewService(
            ReviewRepository reviewRepository,
            BookRepository bookRepository,
            UserRepository userRepository,
            ModerationService moderationService
    ) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.moderationService = moderationService;
    }

    @Transactional
    public ReviewResponse saveMyReview(UserEntity user, UUID bookId, ReviewSaveRequest request) {
        validateMyBook(user, bookId);
        if (request.isPublic()) {
            moderationService.validateContentAllowed(request.content(), ContentModerationContext.BOOK_REVIEW);
        }

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

    public List<ReviewResponse> findPublicReviews(UserEntity viewer, UUID bookId) {
        validateBookExists(bookId);

        List<ReviewEntity> reviews = reviewRepository.findAllByBookIdAndIsPublicTrueOrderByUpdatedAtDesc(bookId);
        return toResponses(viewer, reviews);
    }

    public List<ReviewResponse> findPublicReviewsByBookIdentity(
            UserEntity viewer,
            String isbn,
            String title,
            String author
    ) {
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
        return toResponses(viewer, reviews);
    }

    private List<ReviewResponse> toResponses(UserEntity viewer, List<ReviewEntity> reviews) {
        List<ReviewEntity> visibleReviews = reviews.stream()
                .filter(review -> isVisibleReview(viewer, review))
                .toList();

        Map<UUID, UserEntity> owners = userRepository.findAllById(
                        visibleReviews.stream()
                                .map(ReviewEntity::getUserId)
                                .collect(Collectors.toSet())
                )
                .stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return visibleReviews.stream()
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

    private boolean isVisibleReview(UserEntity viewer, ReviewEntity review) {
        UUID ownerId = review.getUserId();
        return !moderationService.hasBlockBetween(viewer.getId(), ownerId)
                && !moderationService.shouldHideTargetForUser(
                viewer.getId(),
                ModerationTargetType.PUBLIC_REVIEW,
                review.getId().toString()
        );
    }
}
