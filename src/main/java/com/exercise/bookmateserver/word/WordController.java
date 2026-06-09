package com.exercise.bookmateserver.word;

import com.exercise.bookmateserver.auth.CurrentUserResolver;
import com.exercise.bookmateserver.user.UserEntity;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Words", description = "저장 단어 API")
@RestController
public class WordController {

    private final WordService wordService;
    private final CurrentUserResolver currentUserResolver;

    public WordController(WordService wordService, CurrentUserResolver currentUserResolver) {
        this.wordService = wordService;
        this.currentUserResolver = currentUserResolver;
    }

    @Operation(summary = "단어 저장", description = "사전에서 검색한 단어를 특정 책에 저장합니다.")
    @PostMapping("/api/words")
    @ResponseStatus(HttpStatus.CREATED)
    public WordResponse saveWord(HttpServletRequest httpRequest, @Valid @RequestBody WordCreateRequest request) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return wordService.saveWord(user, request);
    }

    @Operation(summary = "전체 저장 단어 조회", description = "저장된 모든 단어를 최신순으로 조회합니다.")
    @GetMapping("/api/words")
    public List<WordResponse> findWords(HttpServletRequest request) {
        UserEntity user = currentUserResolver.get(request);
        return wordService.findWords(user);
    }

    @Operation(summary = "책별 저장 단어 조회", description = "특정 책에 저장된 단어만 조회합니다.")
    @GetMapping("/api/books/{bookId}/words")
    public List<WordResponse> findWordsByBookId(HttpServletRequest request, @PathVariable UUID bookId) {
        UserEntity user = currentUserResolver.get(request);
        return wordService.findWordsByBookId(user, bookId);
    }

    @Operation(summary = "단어 수정", description = "wordId에 해당하는 저장 단어를 수정합니다.")
    @PatchMapping("/api/words/{wordId}")
    public WordResponse updateWord(
            HttpServletRequest httpRequest,
            @PathVariable UUID wordId,
            @Valid @RequestBody WordUpdateRequest request
    ) {
        UserEntity user = currentUserResolver.get(httpRequest);
        return wordService.updateWord(user, wordId, request);
    }

    @Operation(summary = "단어 삭제", description = "wordId에 해당하는 저장 단어를 삭제합니다.")
    @DeleteMapping("/api/words/{wordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWord(HttpServletRequest request, @PathVariable UUID wordId) {
        UserEntity user = currentUserResolver.get(request);
        wordService.deleteWord(user, wordId);
    }
}
