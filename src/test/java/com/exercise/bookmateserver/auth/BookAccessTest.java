package com.exercise.bookmateserver.auth;

import com.exercise.bookmateserver.book.*;
import com.exercise.bookmateserver.user.*;
import com.exercise.bookmateserver.word.WordRepository;
import com.exercise.bookmateserver.quote.QuoteRepository;
import com.exercise.bookmateserver.review.ReviewRepository;
import com.exercise.bookmateserver.readingmemo.ReadingMemoRepository;
import com.exercise.bookmateserver.moderation.ContentModerationPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookAccessTest {
    @Test
    void rejectsAnonymousAndForeignBookRequests() throws Exception {
        UUID viewerId = UUID.randomUUID();
        UUID foreignBookId = UUID.randomUUID();
        TokenService tokens = mock(TokenService.class);
        UserRepository users = mock(UserRepository.class);
        BookRepository books = mock(BookRepository.class);
        UserEntity viewer = mock(UserEntity.class);
        when(viewer.getId()).thenReturn(viewerId);
        when(tokens.parseUserId("viewer-token")).thenReturn(viewerId);
        when(users.findById(viewerId)).thenReturn(Optional.of(viewer));
        when(books.findByIdAndUserId(foreignBookId, viewerId)).thenReturn(Optional.empty());
        BookService service = new BookService(books, mock(WordRepository.class), mock(QuoteRepository.class),
                mock(ReviewRepository.class), mock(ReadingMemoRepository.class), mock(ContentModerationPolicy.class));
        var mvc = MockMvcBuilders.standaloneSetup(new BookController(service, new CurrentUserResolver()))
                .addInterceptors(new AuthInterceptor(tokens, users)).build();
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books/" + foreignBookId).header("Authorization", "Bearer viewer-token"))
                .andExpect(status().isNotFound());
    }
}
