package com.exercise.bookmateserver.ratelimit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LimitedWrite {
    Kind value();

    enum Kind {
        PROFILE_IMAGE(3, 20), GUESTBOOK(10, 100), RECORD(60, 1000);
        final int perMinute;
        final int perDay;
        Kind(int perMinute, int perDay) {
            this.perMinute = perMinute;
            this.perDay = perDay;
        }
    }
}
