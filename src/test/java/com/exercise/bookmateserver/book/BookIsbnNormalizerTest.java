package com.exercise.bookmateserver.book;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookIsbnNormalizerTest {

    @Test
    void normalizePrefersIsbn13WhenKakaoReturnsMultipleValues() {
        String normalized = BookIsbnNormalizer.normalize("8972756197 9788972756194");

        assertEquals("9788972756194", normalized);
    }

    @Test
    void normalizeReturnsNullWhenIsbnIsBlank() {
        assertNull(BookIsbnNormalizer.normalize("   "));
    }
}
