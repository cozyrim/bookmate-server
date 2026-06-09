package com.exercise.bookmateserver.word;

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
public class WordService {

    private final WordRepository wordRepository;
    private final BookRepository bookRepository;

    public WordService(WordRepository wordRepository, BookRepository bookRepository) {
        this.wordRepository = wordRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public WordResponse saveWord(UserEntity user, WordCreateRequest request) {
        validateBookOwner(user, request.bookId());

        WordEntity word = new WordEntity(
                user.getId(),
                request.bookId(),
                request.text(),
                request.meaning(),
                request.partOfSpeech(),
                request.exampleSentence(),
                request.targetCode()
        );

        WordEntity savedWord = wordRepository.save(word);

        return WordResponse.from(savedWord);
    }

    public List<WordResponse> findWords() {
        return wordRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(WordResponse::from)
                .toList();
    }

    public List<WordResponse> findWords(UserEntity user) {
        return wordRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(WordResponse::from)
                .toList();
    }

    public List<WordResponse> findWordsByBookId(UserEntity user, UUID bookId) {
        validateBookOwner(user, bookId);

        return wordRepository.findByBookIdAndUserIdOrderByCreatedAtDesc(bookId, user.getId())
                .stream()
                .map(WordResponse::from)
                .toList();
    }

    @Transactional
    public WordResponse updateWord(UserEntity user, UUID wordId, WordUpdateRequest request) {
        validateBookOwner(user, request.bookId());

        WordEntity word = wordRepository.findByIdAndUserId(wordId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "단어를 찾을 수 없습니다."));

        word.update(
                request.bookId(),
                request.text(),
                request.meaning(),
                request.partOfSpeech(),
                request.exampleSentence(),
                request.targetCode()
        );

        return WordResponse.from(word);
    }

    @Transactional
    public void deleteWord(UserEntity user, UUID wordId) {
        WordEntity word = wordRepository.findByIdAndUserId(wordId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "단어를 찾을 수 없습니다."));

        wordRepository.delete(word);
    }

    private void validateBookOwner(UserEntity user, UUID bookId) {
        bookRepository.findByIdAndUserId(bookId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."));
    }
}
