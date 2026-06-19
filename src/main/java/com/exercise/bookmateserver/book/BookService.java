package com.exercise.bookmateserver.book;

import com.exercise.bookmateserver.quote.QuoteRepository;
import com.exercise.bookmateserver.review.ReviewRepository;
import com.exercise.bookmateserver.word.WordRepository;
import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.moderation.ContentModerationContext;
import com.exercise.bookmateserver.moderation.ContentModerationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final WordRepository wordRepository;
    private final QuoteRepository quoteRepository;
    private final ReviewRepository reviewRepository;
    private final com.exercise.bookmateserver.readingmemo.ReadingMemoRepository readingMemoRepository;
    private final ContentModerationPolicy contentModerationPolicy;

    public BookService(
            BookRepository bookRepository,
            WordRepository wordRepository,
            QuoteRepository quoteRepository,
            ReviewRepository reviewRepository,
            com.exercise.bookmateserver.readingmemo.ReadingMemoRepository readingMemoRepository,
            ContentModerationPolicy contentModerationPolicy
    ) {
        this.bookRepository = bookRepository;
        this.wordRepository = wordRepository;
        this.quoteRepository = quoteRepository;
        this.reviewRepository = reviewRepository;
        this.readingMemoRepository = readingMemoRepository;
        this.contentModerationPolicy = contentModerationPolicy;
    }

    @Transactional
    public BookResponse createBook(UserEntity user, BookCreateRequest request) {
        BookEntity book = new BookEntity(
                user.getId(),
                request.title(),
                request.author(),
                request.imageName(),
                request.isbn(),
                request.progress(),
                request.category(),
                request.totalPages(),
                request.currentPage()
        );

        BookEntity savedBook = bookRepository.save(book);

        return BookResponse.from(savedBook);
    }

    public List<BookResponse> findBooks() {
        return bookRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(BookResponse::from)
                .toList();
    }

    public List<BookResponse> findBooks(UserEntity user) {
        return bookRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(BookResponse::from)
                .toList();
    }

    public BookResponse findBook(UserEntity user, UUID bookId) {
        BookEntity book = bookRepository.findByIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        return BookResponse.from(book);
    }

    @Transactional
    public BookResponse updateBook(UserEntity user, UUID bookId, BookUpdateRequest request) {
        BookEntity book = bookRepository.findByIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        contentModerationPolicy.validateAllowed(request.review(), ContentModerationContext.BOOK_REVIEW);

        book.update(
                request.title(),
                request.author(),
                request.imageName(),
                request.isbn(),
                request.progress(),
                request.category(),
                request.totalPages(),
                request.currentPage(),
                request.rating(),
                request.review(),
                request.readingStatus(),
                request.startDate(),
                request.endDate()
        );

        return BookResponse.from(book);
    }

    @Transactional
    public void deleteBook(UserEntity user, UUID bookId) {
        BookEntity book = bookRepository.findByIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        wordRepository.deleteByBookIdAndUserId(bookId, user.getId());
        quoteRepository.deleteByBookIdAndUserId(bookId, user.getId());  // 구절도 함께 삭제
        reviewRepository.deleteByBookIdAndUserId(bookId, user.getId()); // 리뷰도 함께 삭제
        readingMemoRepository.deleteByBookIdAndUserId(bookId, user.getId()); // 메모도 함께 삭제
        bookRepository.delete(book);
    }
}
