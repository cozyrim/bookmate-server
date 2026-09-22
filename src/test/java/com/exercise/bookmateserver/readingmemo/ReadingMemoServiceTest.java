package com.exercise.bookmateserver.readingmemo;

import com.exercise.bookmateserver.book.BookRepository;
import com.exercise.bookmateserver.book.BookEntity;
import com.exercise.bookmateserver.user.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ReadingMemoServiceTest {
    @Test
    void rejectsMemoForAnotherUsersBookWithoutSaving() {
        BookRepository books = mock(BookRepository.class);
        ReadingMemoRepository memos = mock(ReadingMemoRepository.class);
        UserEntity user = mock(UserEntity.class);
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(books.findByIdAndUserId(bookId, userId)).thenReturn(Optional.empty());
        ReadingMemoService service = new ReadingMemoService(memos, books);
        assertThatThrownBy(() -> service.createMemo(user,
                new ReadingMemoCreateRequest(bookId, "2026-09-22", 1, "메모")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        error -> assertThat(error.getStatusCode().value()).isEqualTo(404));
        verify(memos, never()).save(any());
    }

    @Test
    void permitsMemoForOwnedBook() {
        BookRepository books = mock(BookRepository.class);
        ReadingMemoRepository memos = mock(ReadingMemoRepository.class);
        UserEntity user = mock(UserEntity.class);
        UUID userId = UUID.randomUUID();
        UUID bookId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(books.findByIdAndUserId(bookId, userId)).thenReturn(Optional.of(mock(BookEntity.class)));
        when(memos.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new ReadingMemoService(memos, books).createMemo(user,
                new ReadingMemoCreateRequest(bookId, "2026-09-22", 1, "메모"));
        verify(memos).save(any());
    }
}
