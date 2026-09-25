package com.exercise.bookmateserver.ratelimit;

import com.exercise.bookmateserver.user.UserEntity;
import com.exercise.bookmateserver.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class UserWriteLimitIntegrationTest {
    @Autowired UserWriteLimitService limits;
    @Autowired UserRepository users;
    @Autowired UserWriteLimitRepository counters;
    @Autowired JdbcTemplate jdbc;

    private UUID user() {
        return users.saveAndFlush(UserEntity.createLocal(UUID.randomUUID()+"@example.invalid", "unused",
                UUID.randomUUID().toString().substring(0,8), null)).getId();
    }

    @Test
    void uploadBudgetIsPerUserAndIndependentOfRecordWrites() {
        UUID first = user();
        for (int i=0;i<3;i++) assertThat(limits.consume(first, LimitedWrite.Kind.PROFILE_IMAGE)).isZero();
        assertThat(limits.consume(first, LimitedWrite.Kind.PROFILE_IMAGE)).isBetween(1L,60L);
        assertThat(limits.consume(user(), LimitedWrite.Kind.PROFILE_IMAGE)).isZero();
        assertThat(limits.consume(first, LimitedWrite.Kind.RECORD)).isZero();
    }

    @Test
    void expiredMinuteWindowResetsButDailyBudgetStillApplies() {
        UUID id = user();
        String prefix = id+":PROFILE_IMAGE";
        UserWriteLimit minute = new UserWriteLimit(prefix+":minute");
        for(int i=0;i<3;i++) minute.consume(Instant.now().minusSeconds(120).getEpochSecond()/60*60);
        counters.saveAndFlush(minute);
        assertThat(limits.consume(id, LimitedWrite.Kind.PROFILE_IMAGE)).isZero();
        UserWriteLimit daily = counters.findById(prefix+":day").orElseThrow();
        long today = Instant.now().getEpochSecond()/86400*86400;
        for(int i=1;i<20;i++) daily.consume(today);
        counters.flush();
        assertThat(limits.consume(id, LimitedWrite.Kind.PROFILE_IMAGE)).isBetween(1L,86400L);
    }
}
