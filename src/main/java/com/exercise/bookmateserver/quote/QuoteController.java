package com.exercise.bookmateserver.quote;

import com.exercise.bookmateserver.ratelimit.LimitedWrite;

import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Quotes", description = "기억에 남는 구절 API")
@RestController
public class QuoteController {

    private final QuoteService quoteService;
    private final CurrentUserResolver currentUserResolver;

    public QuoteController(QuoteService quoteService, CurrentUserResolver currentUserResolver) {
        this.quoteService = quoteService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "구절 저장", description = "특정 책의 구절을 저장합니다.")
    @LimitedWrite(LimitedWrite.Kind.RECORD)
    @PostMapping("/api/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    public QuoteResponse saveQuote(HttpServletRequest httpRequest, @Valid @RequestBody QuoteCreateRequest request) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return quoteService.saveQuote(user, request);
    }

    @Operation(summary = "책별 구절 조회", description = "특정 책에 저장된 구절 목록을 최신순으로 조회합니다.")
    @GetMapping("/api/books/{bookId}/quotes")
    public List<QuoteResponse> findQuotesByBookId(HttpServletRequest request, @PathVariable UUID bookId) {
        UserEntity user = currentUserResolver.get(request);
        return quoteService.findQuotesByBookId(user, bookId);
    }

    @Operation(summary = "구절 수정", description = "quoteId에 해당하는 구절을 수정합니다.")
    @PatchMapping("/api/quotes/{quoteId}")
    public QuoteResponse updateQuote(
            HttpServletRequest httpRequest,
            @PathVariable UUID quoteId,
            @Valid @RequestBody QuoteUpdateRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return quoteService.updateQuote(user, quoteId, request);
    }

    @Operation(summary = "구절 삭제", description = "quoteId에 해당하는 구절을 삭제합니다.")
    @DeleteMapping("/api/quotes/{quoteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuote(HttpServletRequest request, @PathVariable UUID quoteId) {
        UserEntity user = currentUserResolver.get(request);
        quoteService.deleteQuote(user, quoteId);
    }
}
