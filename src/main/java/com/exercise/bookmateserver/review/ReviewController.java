package com.exercise.bookmateserver.review;

import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Reviews", description = "책 리뷰 API")
@RestController
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserResolver currentUserResolver;

    public ReviewController(ReviewService reviewService, CurrentUserResolver currentUserResolver) {
        this.reviewService = reviewService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "내 리뷰 저장", description = "bookId에 해당하는 내 책 리뷰를 생성하거나 수정합니다.")
    @PutMapping("/api/books/{bookId}/reviews/me")
    public ReviewResponse saveMyReview(
            HttpServletRequest httpRequest,
            @PathVariable UUID bookId,
            @Valid @RequestBody ReviewSaveRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return reviewService.saveMyReview(user, bookId, request);
    }

    @Operation(summary = "내 리뷰 조회", description = "bookId에 해당하는 내 리뷰를 조회합니다.")
    @GetMapping("/api/books/{bookId}/reviews/me")
    public ResponseEntity<ReviewResponse> findMyReview(
            HttpServletRequest httpRequest,
            @PathVariable UUID bookId
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return reviewService.findMyReview(user, bookId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "공개 리뷰 조회", description = "bookId에 해당하는 공개 리뷰 목록을 최신 수정순으로 조회합니다.")
    @GetMapping("/api/books/{bookId}/reviews")
    public List<ReviewResponse> findPublicReviews(@PathVariable UUID bookId) {
        return reviewService.findPublicReviews(bookId);
    }

    @Operation(summary = "책 검색용 공개 리뷰 조회", description = "ISBN 또는 제목/저자에 해당하는 책들의 공개 리뷰 목록을 최신 수정순으로 조회합니다.")
    @GetMapping("/api/reviews/public")
    public List<ReviewResponse> findPublicReviewsByBookIdentity(
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author
    ) {
        return reviewService.findPublicReviewsByBookIdentity(isbn, title, author);
    }

    @Operation(summary = "내 리뷰 삭제", description = "bookId에 해당하는 내 리뷰를 삭제합니다.")
    @DeleteMapping("/api/books/{bookId}/reviews/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyReview(
            HttpServletRequest httpRequest,
            @PathVariable UUID bookId
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        reviewService.deleteMyReview(user, bookId);
    }
}
