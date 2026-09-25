package com.exercise.bookmateserver.book;

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

@Tag(name = "Books", description = "책 등록 및 조회 API")
@RestController
public class BookController {

    private final BookService bookService;
    private final CurrentUserResolver currentUserResolver;

    public BookController(BookService bookService, CurrentUserResolver currentUserResolver) {
        this.bookService = bookService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "책 등록", description = "새로운 책을 등록합니다.")
    @LimitedWrite(LimitedWrite.Kind.RECORD)
    @PostMapping("/api/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(HttpServletRequest httpRequest, @Valid @RequestBody BookCreateRequest request) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return bookService.createBook(user, request);
    }

    @Operation(summary = "전체 책 조회", description = "등록된 모든 책을 최신순으로 조회합니다.")
    @GetMapping("/api/books")
    public List<BookResponse> findBooks(HttpServletRequest request) {
        UserEntity user = currentUserResolver.get(request);
        return bookService.findBooks(user);
    }

    @Operation(summary = "책 단건 조회", description = "bookId에 해당하는 책 하나를 조회합니다.")
    @GetMapping("/api/books/{bookId}")
    public BookResponse findBook(HttpServletRequest request, @PathVariable UUID bookId) {
        UserEntity user = currentUserResolver.get(request);
        return bookService.findBook(user, bookId);
    }

    @Operation(summary = "책 수정", description = "bookId에 해당하는 책 정보를 수정합니다.")
    @PatchMapping("/api/books/{bookId}")
    public BookResponse updateBook(
            HttpServletRequest httpRequest,
            @PathVariable UUID bookId,
            @Valid @RequestBody BookUpdateRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return bookService.updateBook(user, bookId, request);
    }

    @Operation(summary = "책 삭제", description = "bookId에 해당하는 책과 책에 저장된 단어를 함께 삭제합니다.")
    @DeleteMapping("/api/books/{bookId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(HttpServletRequest request, @PathVariable UUID bookId) {
        UserEntity user = currentUserResolver.get(request);
        bookService.deleteBook(user, bookId);
    }
}
