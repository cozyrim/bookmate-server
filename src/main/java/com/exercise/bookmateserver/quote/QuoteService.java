package com.exercise.bookmateserver.quote;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.user.UserEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final BookRepository bookRepository;

    public QuoteService(QuoteRepository quoteRepository, BookRepository bookRepository) {
        this.quoteRepository = quoteRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public QuoteResponse saveQuote(UserEntity user, QuoteCreateRequest request) {
        validateBookOwner(user, request.bookId());

        QuoteEntity quote = new QuoteEntity(
                user.getId(),
                request.bookId(),
                request.text(),
                request.page(),
                request.memo()
        );

        QuoteEntity savedQuote = quoteRepository.save(quote);

        return QuoteResponse.from(savedQuote);
    }

    public List<QuoteResponse> findQuotesByBookId(UserEntity user, UUID bookId) {
        validateBookOwner(user, bookId);

        return quoteRepository.findByBookIdAndUserIdOrderByCreatedAtDesc(bookId, user.getId())
                .stream()
                .map(QuoteResponse::from)
                .toList();
    }

    @Transactional
    public QuoteResponse updateQuote(UserEntity user, UUID quoteId, QuoteUpdateRequest request) {
        validateBookOwner(user, request.bookId());

        QuoteEntity quote = quoteRepository.findByIdAndUserId(quoteId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "구절을 찾을 수 없습니다."));

        quote.update(
                request.bookId(),
                request.text(),
                request.page(),
                request.memo()
        );

        return QuoteResponse.from(quote);
    }

    @Transactional
    public void deleteQuote(UserEntity user, UUID quoteId) {
        QuoteEntity quote = quoteRepository.findByIdAndUserId(quoteId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "구절을 찾을 수 없습니다."));

        quoteRepository.delete(quote);
    }

    private void validateBookOwner(UserEntity user, UUID bookId) {
        bookRepository.findByIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));
    }
}
