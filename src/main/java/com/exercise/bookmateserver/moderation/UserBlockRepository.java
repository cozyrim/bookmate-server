package com.exercise.bookmateserver.moderation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlockEntity, UUID> {

    Optional<UserBlockEntity> findByBlockerUserIdAndBlockedUserId(UUID blockerUserId, UUID blockedUserId);

    boolean existsByBlockerUserIdAndBlockedUserId(UUID blockerUserId, UUID blockedUserId);

    List<UserBlockEntity> findAllByBlockerUserIdOrderByCreatedAtDesc(UUID blockerUserId);

    void deleteByBlockerUserIdAndBlockedUserId(UUID blockerUserId, UUID blockedUserId);

    void deleteByBlockerUserIdOrBlockedUserId(UUID blockerUserId, UUID blockedUserId);

    @Query("""
            select b.blockedUserId
            from UserBlockEntity b
            where b.blockerUserId = :blockerUserId
            """)
    List<UUID> findBlockedUserIdsByBlockerUserId(@Param("blockerUserId") UUID blockerUserId);

    @Query("""
            select count(b) > 0
            from UserBlockEntity b
            where (b.blockerUserId = :firstUserId and b.blockedUserId = :secondUserId)
               or (b.blockerUserId = :secondUserId and b.blockedUserId = :firstUserId)
            """)
    boolean existsBlockBetween(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId
    );
}
