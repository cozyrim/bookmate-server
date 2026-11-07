package com.exercise.bookmateserver.discovery;

import java.util.List;

public record BestsellerResponse(String source, String fetchedAt, List<Item> items) {
    public record Item(int rank, String id, String title, String author, String publisher,
                       String isbn, String cover, String productUrl, Integer totalPages, String introduction) {}
}
