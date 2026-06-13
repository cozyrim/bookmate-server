package com.exercise.bookmateserver.book;

public final class BookIsbnNormalizer {

    private BookIsbnNormalizer() {
    }

    public static String normalize(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return null;
        }

        String[] candidates = isbn.trim().split("\\s+");
        String bestCandidate = null;

        for (String candidate : candidates) {
            String cleaned = candidate.replaceAll("[^0-9Xx]", "").toUpperCase();
            if (cleaned.length() == 13) {
                return cleaned;
            }

            if (bestCandidate == null && cleaned.length() == 10) {
                bestCandidate = cleaned;
            }
        }

        return bestCandidate;
    }
}
