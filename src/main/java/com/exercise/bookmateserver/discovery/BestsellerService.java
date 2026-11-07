package com.exercise.bookmateserver.discovery;

import org.springframework.stereotype.Service;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class BestsellerService {
    private final Yes24BestsellerClient client;
    private final Clock clock;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private record Cached(BestsellerResponse response, Instant expiresAt) {}
    private volatile Cached cached;
    private volatile Instant retryAt = Instant.MIN;

    @org.springframework.beans.factory.annotation.Autowired
    public BestsellerService(Yes24BestsellerClient client) { this(client, Clock.systemUTC()); }
    BestsellerService(Yes24BestsellerClient client, Clock clock) { this.client = client; this.clock = clock; }

    public BestsellerResponse realtime() {
        var current = cached;
        if (current != null && clock.instant().isBefore(current.expiresAt())) return current.response();
        // Do not consume servlet threads waiting behind a slow provider request.
        if (!refreshLock.tryLock()) throw Yes24BestsellerClient.unavailable();
        try {
            Instant now = clock.instant();
            current = cached;
            if (current != null && now.isBefore(current.expiresAt())) return current.response();
            if (now.isBefore(retryAt)) throw Yes24BestsellerClient.unavailable();
            try {
                var items = client.fetch();
                Instant fetchedAt = clock.instant();
                var response = new BestsellerResponse("YES24", fetchedAt.toString(), items);
                cached = new Cached(response, fetchedAt.plusSeconds(60));
                return response;
            } catch (RuntimeException e) {
                retryAt = clock.instant().plusSeconds(15);
                throw Yes24BestsellerClient.unavailable();
            }
        } finally {
            refreshLock.unlock();
        }
    }
}
