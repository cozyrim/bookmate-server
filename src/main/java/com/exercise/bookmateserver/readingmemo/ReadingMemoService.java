package com.exercise.bookmateserver.readingmemo;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.book.BookRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReadingMemoService {

    private final ReadingMemoRepository readingMemoRepository;
    private final BookRepository bookRepository;

    public ReadingMemoService(ReadingMemoRepository readingMemoRepository, BookRepository bookRepository) {
        this.readingMemoRepository = readingMemoRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public ReadingMemoResponse createMemo(UserEntity user, ReadingMemoCreateRequest request) {
        bookRepository.findByIdAndUserId(request.bookId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));

        ReadingMemoEntity memo = new ReadingMemoEntity(
                user.getId(),
                request.bookId(),
                request.date(),
                request.page(),
                request.text()
        );

        ReadingMemoEntity savedMemo = readingMemoRepository.save(memo);
        return ReadingMemoResponse.from(savedMemo);
    }

    public List<ReadingMemoResponse> findMemos(UserEntity user, UUID bookId) {
        return readingMemoRepository.findAllByBookIdAndUserIdOrderByDateAscCreatedAtAsc(bookId, user.getId())
                .stream()
                .map(ReadingMemoResponse::from)
                .toList();
    }

    @Transactional
    public ReadingMemoResponse updateMemo(UserEntity user, UUID memoId, ReadingMemoUpdateRequest request) {
        ReadingMemoEntity memo = readingMemoRepository.findByIdAndUserId(memoId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메모를 찾을 수 없습니다."));

        memo.update(
                request.date(),
                request.page(),
                request.text()
        );

        return ReadingMemoResponse.from(memo);
    }

    @Transactional
    public void deleteMemo(UserEntity user, UUID memoId) {
        ReadingMemoEntity memo = readingMemoRepository.findByIdAndUserId(memoId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메모를 찾을 수 없습니다."));

        readingMemoRepository.delete(memo);
    }
}
