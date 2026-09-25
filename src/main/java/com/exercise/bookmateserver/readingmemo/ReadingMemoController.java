package com.exercise.bookmateserver.readingmemo;

import com.exercise.bookmateserver.ratelimit.LimitedWrite;

import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reading-memos")
public class ReadingMemoController {

    private final ReadingMemoService readingMemoService;
    private final CurrentUserResolver currentUserResolver;

    public ReadingMemoController(ReadingMemoService readingMemoService, CurrentUserResolver currentUserResolver) {
        this.readingMemoService = readingMemoService;
        this.currentUserResolver = currentUserResolver;
    }

    @LimitedWrite(LimitedWrite.Kind.RECORD)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReadingMemoResponse createMemo(
            HttpServletRequest httpRequest,
            @Valid @RequestBody ReadingMemoCreateRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return readingMemoService.createMemo(user, request);
    }

    @GetMapping
    public List<ReadingMemoResponse> getMemos(
            HttpServletRequest httpRequest,
            @RequestParam UUID bookId
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return readingMemoService.findMemos(user, bookId);
    }

    @PatchMapping("/{memoId}")
    public ReadingMemoResponse updateMemo(
            HttpServletRequest httpRequest,
            @PathVariable UUID memoId,
            @Valid @RequestBody ReadingMemoUpdateRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return readingMemoService.updateMemo(user, memoId, request);
    }

    @DeleteMapping("/{memoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMemo(
            HttpServletRequest httpRequest,
            @PathVariable UUID memoId
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        readingMemoService.deleteMemo(user, memoId);
    }
}
