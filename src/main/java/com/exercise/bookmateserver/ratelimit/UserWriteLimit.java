package com.exercise.bookmateserver.ratelimit;

import jakarta.persistence.*;

@Entity
@Table(name = "user_write_limits")
class UserWriteLimit {
    @Id
    @Column(length = 80)
    private String id;
    private long windowStart;
    private int used;

    protected UserWriteLimit() {}
    UserWriteLimit(String id) { this.id = id; }

    boolean available(long start, int limit) { return windowStart != start || used < limit; }
    void consume(long start) {
        used = windowStart == start ? used + 1 : 1;
        windowStart = start;
    }
}
