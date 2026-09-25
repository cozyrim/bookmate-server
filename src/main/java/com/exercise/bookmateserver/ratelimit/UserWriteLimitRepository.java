package com.exercise.bookmateserver.ratelimit;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserWriteLimitRepository extends JpaRepository<UserWriteLimit, String> {}
