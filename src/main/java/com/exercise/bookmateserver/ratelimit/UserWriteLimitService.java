package com.exercise.bookmateserver.ratelimit;

import com.exercise.bookmateserver.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.UUID;

@Service
public class UserWriteLimitService {
    private final UserRepository users;
    private final UserWriteLimitRepository limits;

    public UserWriteLimitService(UserRepository users, UserWriteLimitRepository limits) {
        this.users = users;
        this.limits = limits;
    }

    // Commit before the upload/business transaction: failed writes also cost budget.
    // Lock the existing user row so first-time concurrent requests cannot race inserts.
    @Transactional
    public long consume(UUID userId, LimitedWrite.Kind kind) {
        users.findActiveByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        long now = Instant.now().getEpochSecond();
        long minute = now / 60 * 60;
        long day = now / 86400 * 86400;
        String prefix = userId + ":" + kind.name();
        UserWriteLimit shortLimit = limits.findById(prefix + ":minute")
                .orElseGet(() -> new UserWriteLimit(prefix + ":minute"));
        UserWriteLimit dailyLimit = limits.findById(prefix + ":day")
                .orElseGet(() -> new UserWriteLimit(prefix + ":day"));
        if (!dailyLimit.available(day, kind.perDay)) return day + 86400 - now;
        if (!shortLimit.available(minute, kind.perMinute)) return minute + 60 - now;
        shortLimit.consume(minute);
        dailyLimit.consume(day);
        limits.save(shortLimit);
        limits.save(dailyLimit);
        return 0;
    }
}
